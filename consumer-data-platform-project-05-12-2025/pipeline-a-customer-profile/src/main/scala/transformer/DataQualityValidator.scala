package transformer

import config.PipelineConfiguration
import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._

class DataQualityValidator(spark: SparkSession) extends LazyLogging {
  import spark.implicits._

  def validateTransactions(transactions: DataFrame): DataFrame = {
    logger.info("Validating transactions")
    val total = transactions.count()
    logger.info(s"Total transactions before validation: $total")

    val validated = transactions
      .filter($"txn_id".isNotNull)
      .filter($"customer_id".isNotNull && $"product_id".isNotNull)
      .filter($"amount" > 0 && $"qty" > 0)
      .dropDuplicates("txn_id")

    val valid = validated.count()
    val invalid = total - valid
    val rate = if (total == 0) 0.0 else (valid.toDouble / total) * 100.0
    logger.info(f"Transactions valid: $valid, invalid: $invalid, rate: $rate%.2f%%")

    if (PipelineConfiguration.DataQuality.logInvalidRecords && invalid > 0) {
      val invalidSample = transactions.except(validated).limit(100)
      logger.warn(s"Sample invalid transactions (up to 100):")
      invalidSample.show(20, truncate = false)
    }

    if (PipelineConfiguration.DataQuality.failOnQualityBreach && invalid > PipelineConfiguration.DataQuality.maxInvalidRecords) {
      throw new IllegalStateException(s"Data quality breach: $invalid invalid transactions exceed ${PipelineConfiguration.DataQuality.maxInvalidRecords}")
    }

    validated
  }

  def validateCustomers(customers: DataFrame): DataFrame = {
    logger.info("Validating customers")
    val validated = customers
      .filter($"customer_id".isNotNull)
      .filter($"name".isNotNull && length($"name") > 0)
      .filter($"email".isNotNull && $"email".rlike("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"))
      .dropDuplicates("customer_id")

    logger.info(s"Valid customers count: ${validated.count()}")
    validated
  }

  def validateProducts(products: DataFrame): DataFrame = {
    logger.info("Validating products")
    val validated = products
      .filter($"product_id".isNotNull)
      .filter($"category".isNotNull)
      .filter($"price" > 0)
      .dropDuplicates("product_id")

    logger.info(s"Valid products count: ${validated.count()}")
    validated
  }
}
