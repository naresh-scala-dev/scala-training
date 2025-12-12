package transformer

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions._
import org.apache.spark.sql.expressions.Window
import com.typesafe.scalalogging.LazyLogging

class TransactionSummarizer extends LazyLogging {

  def buildDailySummary(txnDF: DataFrame): DataFrame = {
    logger.info("Phase 1: Data Quality - Filtering null product_ids")

    // Data quality check: reject null product_id
    val initialCount = txnDF.count()
    val cleanDF = txnDF.filter(col("product_id").isNotNull)
    val rejectedCount = initialCount - cleanDF.count()

    if (rejectedCount > 0) {
      logger.warn(s"⚠️  Rejected $rejectedCount transactions (null product_id)")
    } else {
      logger.info("✓ All transactions have valid product_id")
    }

    logger.info("Phase 2: Add date partition column")
    // Add date column for partitioning
    val withDate = cleanDF.withColumn("date", to_date(col("txn_timestamp")))

    logger.info("Phase 3: Compute top category per (date, customer_id)")
    // Find top category per customer per day using window function
    val categoryRanked = withDate
      .groupBy("date", "customer_id", "category")
      .agg(count("*").as("category_count"))
      .withColumn(
        "category_rank",
        row_number().over(
          Window.partitionBy("date", "customer_id")
            .orderBy(desc("category_count"))
        )
      )
      .filter(col("category_rank") === 1)
      .select(
        col("date"),
        col("customer_id"),
        col("category").as("top_category")
      )

    logger.info("Phase 4: Aggregate transaction metrics")
    // Aggregate transaction amounts, quantities, distinct products
    val summary = withDate
      .groupBy("date", "customer_id")
      .agg(
        sum("amount").as("total_amount"),
        sum("qty").as("total_items"),
        countDistinct("product_id").as("distinct_products")
      )
      // Join with top category
      .join(categoryRanked, Seq("date", "customer_id"), "left")
      // Reorder columns as per spec
      .select(
        col("date"),
        col("customer_id"),
        col("total_amount").cast("decimal(12,2)").as("total_amount"),
        col("total_items").cast("int").as("total_items"),
        col("distinct_products").cast("int").as("distinct_products"),
        col("top_category").cast("string").as("top_category")
      )

    val summaryCount = summary.count()
    logger.info(s"✓ Generated $summaryCount daily summaries (rows: date + customer_id)")
    summary
  }
}