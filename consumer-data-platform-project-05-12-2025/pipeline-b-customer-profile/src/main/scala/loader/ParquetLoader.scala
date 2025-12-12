package loader

import org.apache.spark.sql.{DataFrame, SaveMode}
import com.typesafe.scalalogging.LazyLogging
import config.PipelineBConfiguration

class ParquetLoader extends LazyLogging {

  def write(df: DataFrame): Unit = {

    logger.info(s"Writing Parquet to ${PipelineBConfiguration.parquet.basePath}")

    // Increase parallelism: use more partitions for parallel writes
    val parallelism = 16  // Increased from config value

    logger.info(s"  - Repartition parallelism: $parallelism")
    logger.info(s"  - Compression: Snappy")
    logger.info(s"  - Partition column: date")

    df
      .repartition(parallelism, org.apache.spark.sql.functions.col("date"))
      .write
      .mode(SaveMode.Append)
      .option("compression", "snappy")
      .option("parquet.compression", "snappy")
      .partitionBy("date")
      .parquet(PipelineBConfiguration.parquet.basePath)

    logger.info("✓ Parquet write completed successfully")
  }

  /**
   * Validate Parquet output by reading back partition folders
   */
  def validateOutput(spark: org.apache.spark.sql.SparkSession): Unit = {
    logger.info("Validating Parquet output...")
    try {
      val df = spark.read.parquet(PipelineBConfiguration.parquet.basePath)
      val rowCount = df.count()
      logger.info(s"✓ Parquet validation passed: $rowCount rows")
      logger.info("Sample rows:")
      df.limit(5).show(false)
    } catch {
      case e: Exception =>
        logger.error(s"✗ Parquet validation failed: ${e.getMessage}", e)
    }
  }
}