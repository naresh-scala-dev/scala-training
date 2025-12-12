import org.apache.spark.sql.SparkSession
import com.typesafe.scalalogging.LazyLogging
import consumer.KafkaEventConsumer
import config.AppConfigC

object KafkaToParquetApp extends LazyLogging {

  def main(args: Array[String]): Unit = {

    logger.info("╔════════════════════════════════════════════════════════════╗")
    logger.info("║    Initializing Spark Session for Pipeline C (Optimized)   ║")
    logger.info("╚════════════════════════════════════════════════════════════╝")

    try {
      implicit val spark: SparkSession = SparkSession.builder()
        .appName(AppConfigC.spark.appName)
        .master(AppConfigC.spark.master)
        // Performance configs
        .config("spark.sql.shuffle.partitions", AppConfigC.spark.shufflePartitions)
        .config("spark.sql.streaming.schemaInference", "true")
        .config("spark.streaming.kafka.maxRatePerPartition", "100000")  // Higher throughput
        .config("spark.sql.adaptive.enabled", "false")  // Disable for streaming
        .config("spark.sql.streaming.statefulOperator.checkCorrectness.enabled", "false")
        .getOrCreate()

      spark.sparkContext.setLogLevel("WARN")  // Less verbose logging for performance

      logger.info(s"Spark version: ${spark.version}")
      logger.info(s"App name: ${AppConfigC.spark.appName}")
      logger.info(s"Master: ${AppConfigC.spark.master}")
      logger.info(s"Kafka brokers: ${AppConfigC.kafka.bootstrapServers}")
      logger.info(s"Kafka topic: ${AppConfigC.kafka.topic}")
      logger.info(s"Parquet output: ${AppConfigC.parquet.basePath}")
      logger.info(s"Checkpoint: ${AppConfigC.spark.checkpointLocation}")
      logger.info(s"Shuffle partitions: ${AppConfigC.spark.shufflePartitions}")
      logger.info(s"Writer parallelism: ${AppConfigC.spark.writerParallelism}")

      val consumer = new KafkaEventConsumer(spark)
      consumer.start()

      logger.info("Pipeline C streaming job completed")

    } catch {
      case e: Exception =>
        logger.error(s"FATAL ERROR in Pipeline C: ${e.getMessage}", e)
        throw e
    }
  }
}