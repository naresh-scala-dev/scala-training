package transformer


import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.sql.{DataFrame, SparkSession}

class DataTransformer(spark: SparkSession) extends LazyLogging {
  private val validator = new DataQualityValidator(spark)
  private val aggregator = new ProfileAggregator(spark)

  def transform(customers: DataFrame, transactions: DataFrame, products: DataFrame): DataFrame = {
    logger.info("Starting transformation pipeline")

    val validCustomers = validator.validateCustomers(customers)
    val validProducts = validator.validateProducts(products)
    val validTransactions = validator.validateTransactions(transactions)

    val profiles = aggregator.buildCustomerProfiles(validCustomers, validTransactions, validProducts)

    logger.info("Transformation completed")
    profiles
  }
}
