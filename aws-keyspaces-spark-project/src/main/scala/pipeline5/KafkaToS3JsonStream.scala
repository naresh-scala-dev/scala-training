package pipeline5

import config.AppConfig
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.avro.functions.from_avro
import org.apache.spark.sql.streaming.Trigger

object KafkaToS3JsonStream {

  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder()
      .appName("KafkaToS3JsonStream")
      .master("local[*]")
      .getOrCreate()

    import spark.implicits._

    // Set log level
    spark.sparkContext.setLogLevel("WARN")

    // Configure S3 access (Hadoop configuration)
    val hconf = spark.sparkContext.hadoopConfiguration
    hconf.set("fs.s3a.endpoint", AppConfig.s3.endpoint)
    hconf.set("fs.s3a.region", AppConfig.s3.region)
    hconf.set("fs.s3a.impl", AppConfig.s3.impl)
    hconf.set("fs.s3a.connection.maximum", "100")
    hconf.set("fs.s3a.path.style.access", AppConfig.s3.pathStyleAccess.toString)
    hconf.set("fs.s3a.fast.upload", "true")
    hconf.set("fs.s3a.access.key", AppConfig.s3.accessKey)
    hconf.set("fs.s3a.secret.key", AppConfig.s3.secretKey)
    hconf.set("fs.s3a.aws.credentials.provider", AppConfig.s3.credentialsProvider)

    // Avro schema matching the data from Pipeline 4
    val avroSchema = """{
      "type": "record",
      "name": "Order",
      "namespace": "com.retail",
      "fields": [
        {"name": "order_id", "type": "int"},
        {"name": "customer_id", "type": "int"},
        {"name": "amount", "type": "double"},
        {"name": "created_at", "type": "string"}
      ]
    }"""

    println("\n🚀 Kafka to S3 JSON streaming started")
    println(s"📥 Reading from Kafka topic: ${AppConfig.kafka.topic}")
    println(s"📤 Writing JSON to S3: ${AppConfig.s3.jsonStreamOutputPath}")
    println("Press Ctrl+C to stop\n")

    // Read from Kafka
    val kafkaDF = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", AppConfig.kafka.bootstrapServers)
      .option("subscribe", AppConfig.kafka.topic)
      .option("startingOffsets", "earliest")
      .option("failOnDataLoss", "false")
      .load()

    // Decode Avro messages
    val decodedDF = kafkaDF
      .select(
        from_avro(col("value"), avroSchema).alias("data"),
        col("timestamp").alias("kafka_timestamp")
      )
      .select(
        col("data.*"),
        col("kafka_timestamp")
      )
      .withColumn("processing_time", current_timestamp())

    // Write to S3 as JSON with foreachBatch
    val query = decodedDF.writeStream
      .foreachBatch { (batchDF: DataFrame, batchId: Long) =>

        val count = batchDF.count()

        println(s"\n=== Batch $batchId at ${java.time.LocalDateTime.now()} ===")
        println(s"Records in batch: $count")

        if (count > 0) {
          // Show sample data
          println("Sample data:")
          batchDF.show(5, truncate = false)

          try {
            // Write to S3 in JSON format
            batchDF
              .coalesce(1)
              .write
              .mode("append")
              .json(AppConfig.s3.jsonStreamOutputPath)

            println(s"✓ Successfully wrote $count records to S3")
            println(s"  Location: ${AppConfig.s3.jsonStreamOutputPath}")

          } catch {
            case e: Exception =>
              println(s"✗ Error writing to S3: ${e.getMessage}")
              e.printStackTrace()
          }
        } else {
          println("No new records in this batch")
        }
      }
      .option("checkpointLocation", AppConfig.s3.jsonCheckpointPath)
      .trigger(Trigger.ProcessingTime("10 seconds"))
      .start()

    query.awaitTermination()
  }
}