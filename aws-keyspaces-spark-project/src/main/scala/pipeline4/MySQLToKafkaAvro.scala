package pipeline4

import config.AppConfig
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.avro.functions.to_avro
import org.apache.spark.sql.streaming.Trigger

object MySQLToKafkaAvro {

  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder()
      .appName("MySQLToKafkaAvro")
      .master("local[*]")
      .getOrCreate()

    import spark.implicits._

    // Set log level to reduce noise
    spark.sparkContext.setLogLevel("WARN")

    // Initialize offset tracking
    var lastOffset: Long = 0L

    // Create a heartbeat stream that triggers every 5 seconds
    val heartbeat = spark.readStream
      .format("rate")
      .option("rowsPerSecond", 1)
      .load()

    val query = heartbeat.writeStream
      .foreachBatch { (_: DataFrame, batchId: Long) =>

        println(s"\n=== Batch $batchId at ${java.time.LocalDateTime.now()} ===")
        println(s"Current offset (last order_id): $lastOffset")

        try {
          // Read new orders from MySQL
          val newOrders = spark.read
            .format("jdbc")
            .option("url", AppConfig.mysql.url)
            .option("user", AppConfig.mysql.user)
            .option("password", AppConfig.mysql.password)
            .option("driver", "com.mysql.cj.jdbc.Driver")
            .option("dbtable",
              s"(SELECT * FROM ${AppConfig.mysql.table} WHERE order_id > $lastOffset ORDER BY order_id) AS t")
            .load()

          val count = newOrders.count()

          if (count > 0) {
            println(s"Found $count new orders")

            // Show the data for debugging
            newOrders.show(false)

            // Get the maximum order_id for next batch
            val maxOrderId = newOrders.agg(max("order_id")).first().getInt(0).toLong

            // Convert to Avro format
            val avroDF = newOrders
              .withColumn("created_at", col("created_at").cast("string"))
              .select(
                to_avro(
                  struct(
                    col("order_id"),
                    col("customer_id"),
                    col("amount"),
                    col("created_at")
                  )
                ).alias("value")
              )

            // Write to Kafka
            avroDF.write
              .format("kafka")
              .option("kafka.bootstrap.servers", AppConfig.kafka.bootstrapServers)
              .option("topic", AppConfig.kafka.topic)
              .save()

            // Update offset for next iteration
            lastOffset = maxOrderId

            println(s"✓ Successfully sent $count records to Kafka")
            println(s"✓ Updated offset to: $lastOffset")

          } else {
            println(s"No new orders found (checked for order_id > $lastOffset)")
          }

        } catch {
          case e: Exception =>
            println(s"✗ Error in batch $batchId: ${e.getMessage}")
            e.printStackTrace()
        }
      }
      .trigger(Trigger.ProcessingTime("5 seconds"))
      .start()

    println("\n🚀 MySQL to Kafka streaming started")
    println(s"📊 Polling MySQL table '${AppConfig.mysql.table}' every 5 seconds")
    println(s"📤 Writing to Kafka topic '${AppConfig.kafka.topic}'")
    println("Press Ctrl+C to stop\n")

    query.awaitTermination()
  }
}