package transformer


import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions._

class ProfileAggregator(spark: SparkSession) extends LazyLogging {
  import spark.implicits._

  def buildCustomerProfiles(customers: DataFrame, transactions: DataFrame, products: DataFrame): DataFrame = {
    logger.info("Enriching transactions with product categories and aggregating per customer")

    val enrichedTx = transactions
      .join(products.select($"product_id", $"category"), Seq("product_id"), "inner")
      .select($"txn_id", $"customer_id", $"amount", $"qty", $"txn_timestamp", $"category")

    enrichedTx.persist()
    logger.info(s"Enriched transactions count: ${enrichedTx.count()}")

    val metrics = enrichedTx
      .groupBy($"customer_id")
      .agg(
        sum($"amount").as("total_spend"),
        count($"txn_id").as("total_transactions"),
        min($"txn_timestamp").as("first_purchase"),
        max($"txn_timestamp").as("last_purchase")
      )
      .withColumn("avg_order_value", round($"total_spend" / $"total_transactions", 2))

    metrics.persist()
    logger.info(s"Computed transaction metrics for customers: ${metrics.count()}")

    val favorite = enrichedTx
      .groupBy($"customer_id", $"category")
      .agg(count("*").as("category_count"))
      .withColumn("rn", row_number().over(Window.partitionBy($"customer_id").orderBy(desc("category_count"))))
      .filter($"rn" === 1)
      .select($"customer_id", $"category".as("favorite_category"))

    favorite.persist()
    logger.info(s"Computed favorite categories for customers: ${favorite.count()}")

    val profiles = customers
      .join(metrics, Seq("customer_id"), "inner")
      .join(favorite, Seq("customer_id"), "left")
      .select(
        $"customer_id",
        $"name",
        $"email",
        $"gender",
        $"total_spend",
        $"total_transactions",
        $"avg_order_value",
        $"first_purchase",
        $"last_purchase",
        coalesce($"favorite_category", lit("Unknown")).as("favorite_category")
      )

    logger.info(s"Built profiles count: ${profiles.count()}")

    // unpersist to release memory
    enrichedTx.unpersist()
    metrics.unpersist()
    favorite.unpersist()

    profiles
  }
}

