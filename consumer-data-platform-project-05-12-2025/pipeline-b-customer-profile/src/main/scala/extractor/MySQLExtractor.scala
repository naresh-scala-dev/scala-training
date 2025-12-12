package extractor

import org.apache.spark.sql.{DataFrame, SparkSession}
import com.typesafe.scalalogging.LazyLogging
import config.PipelineBConfiguration
import java.time.LocalDate

class MySQLExtractor(spark: SparkSession) extends LazyLogging {

  /**
   * Extract transactions for a specific date.
   * If date is None, extracts the latest date available in the table.
   * If fullLoad = true, extracts ALL transactions (backfill).
   */
  def extractTransactions(targetDate: Option[LocalDate] = None, fullLoad: Boolean = false): DataFrame = {

    logger.info(s"Extracting transactions...")
    if (fullLoad) {
      logger.info("  Mode: Full load (all transactions)")
    } else if (targetDate.isDefined) {
      logger.info(s"  Mode: Specific date (${targetDate.get})")
    } else {
      logger.info("  Mode: Latest available date")
    }

    val dateFilterClause = if (fullLoad) {
      "" // No filter - get everything
    } else if (targetDate.isDefined) {
      val dateStr = targetDate.get.toString  // YYYY-MM-DD format
      s"AND DATE(t.txn_timestamp) = '$dateStr'"
    } else {
      // Default: latest available date in the table
      "AND DATE(t.txn_timestamp) = (SELECT MAX(DATE(txn_timestamp)) FROM transactions)"
    }

    val query =
      s"""(
         |SELECT t.txn_id,
         |       t.customer_id,
         |       t.product_id,
         |       t.qty,
         |       t.amount,
         |       t.txn_timestamp,
         |       p.category
         |FROM transactions t
         |INNER JOIN products p ON t.product_id = p.product_id
         |WHERE t.product_id IS NOT NULL
         |$dateFilterClause
         |) tx
         |""".stripMargin

    logger.info(s"SQL Query:\n$query")

    val df = spark.read
      .format("jdbc")
      .option("url", PipelineBConfiguration.mysql.url)
      .option("dbtable", query)
      .option("user", PipelineBConfiguration.mysql.user)
      .option("password", PipelineBConfiguration.mysql.password)
      .option("fetchsize", PipelineBConfiguration.mysql.fetchSize)
      .option("partitionColumn", "customer_id")
      .option("numPartitions", PipelineBConfiguration.mysql.numPartitions)
      .option("lowerBound", PipelineBConfiguration.mysql.lowerBound)
      .option("upperBound", PipelineBConfiguration.mysql.upperBound)
      .load()

    val extractedCount = df.count()
    logger.info(s"✓ Extracted $extractedCount transactions")

    if (extractedCount == 0) {
      logger.warn("⚠️  No transactions found for the given date")
    }

    df
  }

  /**
   * Extract products for dimension lookup
   */
  def extractProducts(): DataFrame = {
    logger.info("Reading products reference data from MySQL")

    val df = spark.read
      .format("jdbc")
      .option("url", PipelineBConfiguration.mysql.url)
      .option("dbtable", "products")
      .option("user", PipelineBConfiguration.mysql.user)
      .option("password", PipelineBConfiguration.mysql.password)
      .load()

    logger.info(s"✓ Loaded ${df.count()} products")
    df
  }
}
