package extractor


import config.PipelineConfiguration
import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.sql.{DataFrame, SparkSession}
import java.sql.Timestamp
import java.time.LocalDateTime

/**
 * Single MySQL extractor class with explicit methods for each source table.
 * Uses pushdown filters and partitioning where appropriate.
 */
class MySQLExtractor extends LazyLogging {

  private val jdbcUrl = PipelineConfiguration.MySQL.jdbcUrl
  private val connectionProps = PipelineConfiguration.MySQL.connectionProperties

  def extractCustomers(spark: SparkSession): DataFrame = {
    logger.info("Extracting customers from MySQL")
    val start = System.currentTimeMillis()

    val df = spark.read
      .format("jdbc")
      .option("url", jdbcUrl)
      .option("dbtable", "customers")
      .option("fetchsize", PipelineConfiguration.MySQL.fetchSize)
      .options(connectionProps)
      .load()

    val count = df.count()
    logger.info(s"Extracted $count customers in ${System.currentTimeMillis() - start}ms")
    df
  }

  def extractProducts(spark: SparkSession): DataFrame = {
    logger.info("Extracting products from MySQL")
    val start = System.currentTimeMillis()

    val df = spark.read
      .format("jdbc")
      .option("url", jdbcUrl)
      .option("dbtable", "products")
      .option("fetchsize", PipelineConfiguration.MySQL.fetchSize)
      .options(connectionProps)
      .load()

    val count = df.count()
    logger.info(s"Extracted $count products in ${System.currentTimeMillis() - start}ms")
    df
  }

  def extractTransactions(spark: SparkSession): DataFrame = {
    logger.info("Extracting transactions from MySQL with partitioning and lookback")
    val start = System.currentTimeMillis()

    val lookbackDate = Timestamp.valueOf(LocalDateTime.now().minusYears(PipelineConfiguration.Extraction.transactionLookbackYears))
    logger.info(s"Applying txn_timestamp >= $lookbackDate")

    // Use subquery pushdown for predicate
    val query = s"(SELECT * FROM transactions WHERE txn_timestamp >= '${lookbackDate}') AS filtered_transactions"

    val dfReader = spark.read
      .format("jdbc")
      .option("url", jdbcUrl)
      .option("dbtable", query)
      .option("fetchsize", PipelineConfiguration.MySQL.fetchSize)
      .options(connectionProps)

    // Add partition options if configured
    val dfWithPartitioning = dfReader
      .option("partitionColumn", PipelineConfiguration.MySQL.partitionColumn)
      .option("numPartitions", PipelineConfiguration.MySQL.numPartitions)
      .option("lowerBound", PipelineConfiguration.MySQL.lowerBound)
      .option("upperBound", PipelineConfiguration.MySQL.upperBound)

    val df = dfWithPartitioning.load()

    val count = df.count()
    logger.info(s"Extracted $count transactions in ${System.currentTimeMillis() - start}ms")
    df
  }
}
