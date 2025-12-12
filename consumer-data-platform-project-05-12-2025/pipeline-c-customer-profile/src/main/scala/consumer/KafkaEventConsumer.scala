package consumer

import config.AppConfigC
import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.sql.{DataFrame, SparkSession, functions => F}
import org.apache.spark.sql.streaming.Trigger
import org.apache.spark.sql.avro.from_avro

class KafkaEventConsumer(spark: SparkSession) extends LazyLogging {

  import spark.implicits._

  def start(): Unit = {
    logger.info("╔════════════════════════════════════════════════════════════╗")
    logger.info("║     Pipeline C: Kafka → Parquet (OPTIMIZED STREAMING)      ║")
    logger.info("╚════════════════════════════════════════════════════════════╝")

    // Set Spark streaming configs for performance
    spark.conf.set("spark.sql.shuffle.partitions", AppConfigC.spark.shufflePartitions)
    spark.conf.set("spark.sql.streaming.schemaInference", "true")
    logger.info(s"Shuffle partitions: ${AppConfigC.spark.shufflePartitions}")
    logger.info(s"Writer parallelism: ${AppConfigC.spark.writerParallelism}")

    logger.info("Phase 1: Configuring S3")
    configureS3()

    logger.info("Phase 2: Reading Kafka stream with optimized settings")
    val rawStream = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", AppConfigC.kafka.bootstrapServers)
      .option("subscribe", AppConfigC.kafka.topic)
      .option("startingOffsets", "earliest")  // Read from beginning
      .option("failOnDataLoss", "false")
      .option("maxOffsetsPerTrigger", "500000")  // Larger batches = better throughput
      .load()

    logger.info("Kafka stream loaded successfully")

    logger.info("Phase 3: Deserializing Avro and transforming with repartitioning")
    val eventsDF = deserializeAvro(rawStream)

    logger.info("Phase 4: Applying data quality filters")
    val validDF = eventsDF
      .filter($"customer_id".isNotNull && $"event_type".isNotNull)
      .filter($"event_timestamp".isNotNull)

    logger.info("Phase 5: Pre-partitioning for better write performance")
    // Repartition by event_date BEFORE writing to maximize parallelism
    // NOTE: sortWithinPartitions is NOT allowed on streaming DataFrames
    val repartitionedDF = validDF
      .repartition(AppConfigC.spark.writerParallelism, $"event_date")

    logger.info("Phase 6: Writing to S3 Parquet with optimized settings")
    repartitionedDF.writeStream
      .format("parquet")
      .option("path", AppConfigC.parquet.basePath)
      .option("checkpointLocation", AppConfigC.spark.checkpointLocation)
      .partitionBy("event_date")
      .outputMode("append")
      .trigger(Trigger.ProcessingTime("15 minutes"))
      .option("mergeSchema", "true")
      .option("compression", "snappy")  // Better compression
      .start()
      .awaitTermination()
  }

  private def deserializeAvro(rawStream: DataFrame): DataFrame = {

    logger.info("Deserializing Kafka Avro payload")

    rawStream
      .select(
        from_avro($"value", avroSchema).as("data")
      )
      .select("data.*")
      // Convert event_timestamp from milliseconds (long) to proper Timestamp
      .withColumn(
        "event_timestamp",
        F.col("event_timestamp").cast("long") / 1000
      )
      .withColumn(
        "event_timestamp",
        F.from_unixtime(F.col("event_timestamp")).cast("timestamp")
      )
      // Add ingestion_timestamp = Spark processing time
      .withColumn(
        "ingestion_timestamp",
        F.current_timestamp()
      )
      // Extract event_date for partitioning
      .withColumn(
        "event_date",
        F.to_date(F.col("event_timestamp"))
      )
      // Explicit cast all columns to correct types
      .select(
        F.col("event_id").cast("string").as("event_id"),
        F.col("customer_id").cast("int").as("customer_id"),
        F.col("product_id").cast("int").as("product_id"),
        F.col("event_type").cast("string").as("event_type"),
        F.col("event_timestamp").cast("timestamp").as("event_timestamp"),
        F.col("ingestion_timestamp").cast("timestamp").as("ingestion_timestamp"),
        F.col("event_date").cast("date").as("event_date")
      )
  }

  private def configureS3(): Unit = {
    logger.info("Setting up S3 configuration for Parquet writes")
    val conf = spark.sparkContext.hadoopConfiguration

    conf.set("fs.s3a.endpoint", AppConfigC.parquet.s3.endpoint)
    conf.set("fs.s3a.access.key", AppConfigC.parquet.s3.accessKey)
    conf.set("fs.s3a.secret.key", AppConfigC.parquet.s3.secretKey)
    conf.set("fs.s3a.impl", AppConfigC.parquet.s3.impl)
    conf.set("fs.s3a.credentials.provider", AppConfigC.parquet.s3.credentialsProvider)
    conf.setBoolean("fs.s3a.path.style.access", AppConfigC.parquet.s3.pathStyleAccess)

    // Performance tuning for S3
    conf.set("fs.s3a.threads.max", "16")
    conf.set("fs.s3a.threads.core", "8")
    conf.set("fs.s3a.connection.maximum", "16")
    conf.set("fs.s3a.block.size", "128M")

    logger.info("S3 configuration applied with performance tuning")
  }

  /**
   * Avro schema - matches what Akka producer sends
   */
  private val avroSchema: String =
    """
      |{
      |  "type":"record",
      |  "name":"BehaviouralEvent",
      |  "namespace":"com.retail.events",
      |  "fields":[
      |    {"name":"event_id","type":"string"},
      |    {"name":"customer_id","type":"int"},
      |    {"name":"product_id","type":"int"},
      |    {"name":"event_type","type":"string"},
      |    {"name":"event_timestamp","type":"long"}
      |  ]
      |}
      |""".stripMargin
}