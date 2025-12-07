package pipeline4

import config.AppConfig
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.avro.functions.to_avro
import org.apache.spark.sql.types._
import org.apache.spark.sql.streaming.Trigger

object MySQLToKafkaAvro {
  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder()
      .appName("MySQLToKafkaAvro")
      .master("local[*]")
      .getOrCreate()

    spark.sparkContext.setLogLevel("WARN")

    val avroSchema =
      """
        {
          "type": "record",
          "name": "Order",
          "namespace": "com.retail",
          "fields": [
            {"name": "order_id", "type": "int"},
            {"name": "customer_id", "type": "int"},
            {"name": "amount", "type": "double"},
            {"name": "created_at", "type": "string"}
          ]
        }
      """

    var lastOffset = 0L

    val heartbeat = spark.readStream
      .format("rate")
      .option("rowsPerSecond", 1)
      .load()

    val query = heartbeat.writeStream
      .foreachBatch { (_: DataFrame, batchId: Long) =>

        val newOrders = spark.read
          .format("jdbc")
          .option("url", AppConfig.mysql.url)
          .option("user", AppConfig.mysql.user)
          .option("password", AppConfig.mysql.password)
          .option("driver", "com.mysql.cj.jdbc.Driver")
          .option("dbtable", s"(SELECT * FROM ${AppConfig.mysql.table} WHERE order_id > $lastOffset ORDER BY order_id) t")
          .load()

        val count = newOrders.count()

        if (count > 0) {

          val maxOrderId = newOrders.agg(max("order_id")).first().getInt(0).toLong

          val out = newOrders
            .withColumn("created_at", col("created_at").cast("string"))
            .select(
              to_avro(
                struct(
                  col("order_id"),
                  col("customer_id"),
                  col("amount"),
                  col("created_at")
                ),
                avroSchema
              ).alias("value")
            )

          out.write
            .format("kafka")
            .option("kafka.bootstrap.servers", AppConfig.kafka.bootstrapServers)
            .option("topic", AppConfig.kafka.topic)
            .save()

          lastOffset = maxOrderId
        }
      }
      .trigger(Trigger.ProcessingTime("5 seconds"))
      .start()

    query.awaitTermination()
  }
}
