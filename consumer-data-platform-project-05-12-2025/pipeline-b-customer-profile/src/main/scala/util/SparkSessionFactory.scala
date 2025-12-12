package util

import org.apache.spark.sql.SparkSession
import com.typesafe.scalalogging.LazyLogging
import config.PipelineBConfiguration

object SparkSessionFactory extends LazyLogging {

  def getOrCreateSession(): SparkSession = {
    logger.info("╔════════════════════════════════════════════════════════════╗")
    logger.info("║    Creating Spark Session for Pipeline B (OPTIMIZED)       ║")
    logger.info("╚════════════════════════════════════════════════════════════╝")

    val spark = SparkSession.builder()
      .appName(PipelineBConfiguration.spark.appName)
      .master(PipelineBConfiguration.spark.master)

      // ============================================================
      // PERFORMANCE TUNING FOR BATCH PROCESSING
      // ============================================================
      // Increase shuffle partitions for better parallelism
      .config("spark.sql.shuffle.partitions", "32")  // Increased from config

      // Enable adaptive query execution for optimal joins
      .config("spark.sql.adaptive.enabled", "true")
      .config("spark.sql.adaptive.coalescePartitions.enabled", "true")
      .config("spark.sql.adaptive.skewJoin.enabled", "true")

      // Improve join and aggregation performance
      .config("spark.sql.join.preferSortMergeJoin", "true")
      .config("spark.sql.autoBroadcastJoinThreshold", "100MB")

      // Memory and caching optimization
      .config("spark.memory.fraction", "0.8")
      .config("spark.memory.storageFraction", "0.5")
      .config("spark.sql.shuffle.partitions.adaptive", "true")

      // ============================================================
      // S3 ENDPOINT CONFIGURATION
      // ============================================================
      .config("spark.hadoop.fs.s3a.endpoint", PipelineBConfiguration.parquet.s3Endpoint)
      .config("spark.hadoop.fs.s3a.region", PipelineBConfiguration.parquet.s3Region)
      .config("spark.hadoop.fs.s3a.impl", PipelineBConfiguration.parquet.s3Impl)
      .config("spark.hadoop.fs.s3a.access.key", PipelineBConfiguration.parquet.s3AccessKey)
      .config("spark.hadoop.fs.s3a.secret.key", PipelineBConfiguration.parquet.s3SecretKey)
      .config("spark.hadoop.fs.s3a.aws.credentials.provider", PipelineBConfiguration.parquet.s3CredentialsProvider)
      .config("spark.hadoop.fs.s3a.path.style.access", PipelineBConfiguration.parquet.s3PathStyleAccess)

      // ============================================================
      // S3 PERFORMANCE TUNING
      // ============================================================
      .config("spark.hadoop.fs.s3a.threads.max", "32")  // Increased from 16
      .config("spark.hadoop.fs.s3a.threads.core", "16") // Increased from 8
      .config("spark.hadoop.fs.s3a.connection.maximum", "200") // Increased from 100
      .config("spark.hadoop.fs.s3a.fast.upload", "true")
      .config("spark.hadoop.fs.s3a.block.size", "256M")  // Increased from 128M
      .config("spark.hadoop.fs.s3a.multipart.size", "256M")
      .config("spark.hadoop.fs.s3a.multipart.threshold", "256M")

      // ============================================================
      // JDBC PERFORMANCE
      // ============================================================
      .config("spark.sql.broadcastTimeout", "36000")
      .config("spark.driver.maxResultSize", "4g")

      .getOrCreate()

    spark.sparkContext.setLogLevel("WARN")

    logger.info(s"Spark version: ${spark.version}")
    logger.info(s"App name: ${PipelineBConfiguration.spark.appName}")
    logger.info(s"Master: ${PipelineBConfiguration.spark.master}")
    logger.info(s"Shuffle partitions: 32")
    logger.info(s"S3 threads: 32 max, 16 core")
    logger.info("✓ Spark Session created successfully")

    spark
  }
}