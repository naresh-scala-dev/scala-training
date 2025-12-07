package caching

import config.AppConfig
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.slf4j.LoggerFactory

import java.io.File

case class Sale(customerId: Int, productId: Int, quantity: Int, amount: Double)

object SalesCachingAnalyzer {

  private val logger = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("SalesCachingAnalyzer")
      .master("local[*]")
      .getOrCreate()

    spark.sparkContext.setLogLevel("WARN")

    // Create sample data if not exists
    createSampleDataIfNotExists(spark)

    // Load sales data
    val sales = loadSales(spark)

    logger.info("=" * 80)
    logger.info("SCENARIO 1: WITHOUT CACHING")
    logger.info("=" * 80)
    runWithoutCaching(sales)
    Thread.sleep(40000)
    logger.info("\n" + "=" * 80)
    logger.info("SCENARIO 2: WITH CACHING")
    logger.info("=" * 80)
    runWithCaching(sales)

    // ADD THIS: Keep application alive to view Storage tab
    logger.info("\n" + "=" * 80)
    logger.info("APPLICATION PAUSED - Check Storage tab in Spark UI now!")
    logger.info("Spark UI: http://localhost:4040")
    logger.info("Press Ctrl+C to stop")
    logger.info("=" * 80)

    Thread.sleep(300000) // Sleep for 5 minutes

    spark.stop()
  }

  private def createSampleDataIfNotExists(spark: SparkSession): Unit = {
    val salesPath = AppConfig.paths.sales

    import spark.implicits._

    if (!new File(salesPath).exists()) {
      logger.info(s"Creating sample sales data at $salesPath")

      val sales = Seq(
        // Customer 1 purchases
        Sale(1, 101, 2, 50.0),
        Sale(1, 102, 1, 30.0),
        Sale(1, 103, 3, 90.0),

        // Customer 2 purchases
        Sale(2, 101, 1, 25.0),
        Sale(2, 104, 2, 80.0),
        Sale(2, 105, 1, 40.0),

        // Customer 3 purchases
        Sale(3, 102, 4, 120.0),
        Sale(3, 103, 2, 60.0),
        Sale(3, 101, 1, 25.0),

        // Customer 4 purchases
        Sale(4, 104, 3, 120.0),
        Sale(4, 105, 2, 80.0),
        Sale(4, 106, 1, 50.0),

        // Customer 5 purchases
        Sale(5, 101, 5, 125.0),
        Sale(5, 102, 3, 90.0),
        Sale(5, 107, 2, 100.0),

        // Customer 6 purchases
        Sale(6, 103, 2, 60.0),
        Sale(6, 104, 1, 40.0),
        Sale(6, 108, 4, 200.0),

        // Customer 7 purchases
        Sale(7, 105, 3, 120.0),
        Sale(7, 106, 2, 100.0),
        Sale(7, 101, 1, 25.0),

        // Customer 8 purchases
        Sale(8, 102, 2, 60.0),
        Sale(8, 107, 1, 50.0),
        Sale(8, 108, 3, 150.0),

        // Customer 9 purchases
        Sale(9, 103, 4, 120.0),
        Sale(9, 104, 2, 80.0),
        Sale(9, 109, 1, 70.0),

        // Customer 10 purchases
        Sale(10, 105, 2, 80.0),
        Sale(10, 106, 3, 150.0),
        Sale(10, 110, 1, 60.0)
      ).toDS().toDF()

      sales.write.mode("overwrite").parquet(salesPath)
      logger.info(s"Created ${sales.count()} sample sales records")
    }
  }

  private def loadSales(spark: SparkSession): DataFrame = {
    spark.read.format("parquet").load(AppConfig.paths.sales)
  }

  private def runWithoutCaching(sales: DataFrame): Unit = {
    val startTime = System.currentTimeMillis()

    // Computation 1: Total amount spent per customer
    val start1 = System.currentTimeMillis()
    val customerSpending = computeCustomerSpending(sales)
    customerSpending.write.mode("overwrite").json(AppConfig.paths.customerOutput)
    val end1 = System.currentTimeMillis()
    logger.info(s"Customer spending computation time: ${end1 - start1} ms")

    // Computation 2: Total quantity sold per product
    val start2 = System.currentTimeMillis()
    val productSales = computeProductSales(sales)
    productSales.write.mode("overwrite").json(AppConfig.paths.productOutput)
    val end2 = System.currentTimeMillis()
    logger.info(s"Product sales computation time: ${end2 - start2} ms")

    val totalTime = System.currentTimeMillis() - startTime
    logger.info(s"Total execution time WITHOUT caching: $totalTime ms")
    logger.info(s"Sales DataFrame was read from disk twice (once for each computation)")
  }

  private def runWithCaching(sales: DataFrame): Unit = {
    val startTime = System.currentTimeMillis()

    // Cache the dataset before performing operations
    logger.info("Caching sales dataset in memory...")
    val cachedSales = sales.cache()

    // Trigger caching by performing a count
    val cacheStart = System.currentTimeMillis()
    val recordCount = cachedSales.count()
    val cacheEnd = System.currentTimeMillis()
    logger.info(s"Cached $recordCount records in ${cacheEnd - cacheStart} ms")

    // Computation 1: Total amount spent per customer (uses cached data)
    val start1 = System.currentTimeMillis()
    val customerSpending = computeCustomerSpending(cachedSales)
    customerSpending.write.mode("overwrite").json(AppConfig.paths.customerOutputCached)
    val end1 = System.currentTimeMillis()
    logger.info(s"Customer spending computation time (with cache): ${end1 - start1} ms")

    // Computation 2: Total quantity sold per product (uses cached data)
    val start2 = System.currentTimeMillis()
    val productSales = computeProductSales(cachedSales)
    productSales.write.mode("overwrite").json(AppConfig.paths.productOutputCached)
    val end2 = System.currentTimeMillis()
    logger.info(s"Product sales computation time (with cache): ${end2 - start2} ms")

    val totalTime = System.currentTimeMillis() - startTime
    logger.info(s"Total execution time WITH caching: $totalTime ms")
    logger.info(s"Sales DataFrame was read from memory (cache) for both computations")

    // Unpersist cache to free memory
    //    cachedSales.unpersist()
    logger.info("Cache cleared from memory")
  }

  private def computeCustomerSpending(sales: DataFrame): DataFrame = {
    sales.groupBy("customerId")
      .agg(
        sum("amount").as("total_spent"),
        sum("quantity").as("total_items"),
        count("*").as("number_of_purchases")
      )
      .orderBy(desc("total_spent"))
  }

  private def computeProductSales(sales: DataFrame): DataFrame = {
    sales.groupBy("productId")
      .agg(
        sum("quantity").as("total_quantity_sold"),
        sum("amount").as("total_revenue"),
        count("*").as("number_of_transactions")
      )
      .orderBy(desc("total_quantity_sold"))
  }
}