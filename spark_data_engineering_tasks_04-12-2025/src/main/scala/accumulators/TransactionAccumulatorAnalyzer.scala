package accumulators

import config.AppConfig
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.util.LongAccumulator
import org.slf4j.LoggerFactory

import java.io.File

case class Transaction(transactionId: Int, customerId: Int, amount: Double, category: String)

object TransactionAccumulatorAnalyzer {

  private val logger = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("TransactionAccumulatorAnalyzer")
      .master("local[*]")
      .getOrCreate()

    // Create sample data if not exists
    createSampleDataIfNotExists(spark)

    // Load transactions
    val transactions = loadTransactions(spark)

    // Define threshold
    val threshold = 500.0

    logger.info("=" * 80)
    logger.info(s"ANALYZING TRANSACTIONS WITH THRESHOLD: $threshold")
    logger.info("=" * 80)

    // Create accumulators
    val highValueCounter = spark.sparkContext.longAccumulator("High-Value Transactions")
    val lowValueCounter = spark.sparkContext.longAccumulator("Low-Value Transactions")
    val totalAmountAccumulator = spark.sparkContext.doubleAccumulator("Total Transaction Amount")

    // Process transactions and update accumulators
    processTransactionsWithAccumulator(transactions, threshold, highValueCounter, lowValueCounter, totalAmountAccumulator)

    // Print results (read accumulator values in driver)
    logger.info("\n" + "=" * 80)
    logger.info("ACCUMULATOR RESULTS")
    logger.info("=" * 80)
    logger.info(s"High-Value Transactions (> $threshold): ${highValueCounter.value}")
    logger.info(s"Low-Value Transactions (<= $threshold): ${lowValueCounter.value}")
    logger.info(s"Total Transactions: ${highValueCounter.value + lowValueCounter.value}")
    logger.info(s"Total Transaction Amount: ${totalAmountAccumulator.value}")
    logger.info(s"Percentage High-Value: ${(highValueCounter.value.toDouble / (highValueCounter.value + lowValueCounter.value)) * 100}%")
    logger.info("=" * 80)

    // Verify with SQL aggregation
    verifyWithSQL(transactions, threshold)

    Thread.sleep(90000)
    spark.stop()
  }

  private def createSampleDataIfNotExists(spark: SparkSession): Unit = {
    val transactionsPath = AppConfig.paths.accum_transactions

    import spark.implicits._

    if (!new File(transactionsPath).exists()) {
      logger.info(s"Creating sample transaction data at $transactionsPath")

      val transactions = Seq(
        // High-value transactions (> 500)
        Transaction(1, 101, 750.0, "Electronics"),
        Transaction(2, 102, 1200.0, "Electronics"),
        Transaction(3, 103, 650.0, "Furniture"),
        Transaction(4, 104, 890.0, "Appliances"),
        Transaction(5, 105, 520.0, "Electronics"),
        Transaction(6, 106, 1500.0, "Jewelry"),
        Transaction(7, 107, 680.0, "Furniture"),
        Transaction(8, 108, 950.0, "Electronics"),
        Transaction(9, 109, 710.0, "Appliances"),
        Transaction(10, 110, 1100.0, "Electronics"),

        // Low-value transactions (<= 500)
        Transaction(11, 111, 45.0, "Groceries"),
        Transaction(12, 112, 120.0, "Clothing"),
        Transaction(13, 113, 89.0, "Books"),
        Transaction(14, 114, 250.0, "Clothing"),
        Transaction(15, 115, 78.0, "Groceries"),
        Transaction(16, 116, 340.0, "Books"),
        Transaction(17, 117, 490.0, "Clothing"),
        Transaction(18, 118, 156.0, "Groceries"),
        Transaction(19, 119, 280.0, "Books"),
        Transaction(20, 120, 425.0, "Clothing"),
        Transaction(21, 121, 95.0, "Groceries"),
        Transaction(22, 122, 310.0, "Books"),
        Transaction(23, 123, 175.0, "Clothing"),
        Transaction(24, 124, 460.0, "Electronics"),
        Transaction(25, 125, 220.0, "Groceries")
      ).toDS().toDF()

      transactions.write.mode("overwrite").parquet(transactionsPath)
      logger.info(s"Created ${transactions.count()} sample transaction records")
    }
  }

  private def loadTransactions(spark: SparkSession): DataFrame = {
    spark.read.format("parquet").load(AppConfig.paths.accum_transactions)
  }

  private def processTransactionsWithAccumulator(
                                                  transactions: DataFrame,
                                                  threshold: Double,
                                                  highValueCounter: LongAccumulator,
                                                  lowValueCounter: LongAccumulator,
                                                  totalAmountAccumulator: org.apache.spark.util.DoubleAccumulator
                                                ): Unit = {

    logger.info("Processing transactions in parallel...")

    // Use foreach to update accumulators (action that triggers parallel processing)
    transactions.foreach { row =>
      val amount = row.getAs[Double]("amount")

      // Update accumulators based on threshold
      if (amount > threshold) {
        highValueCounter.add(1)
      } else {
        lowValueCounter.add(1)
      }

      // Accumulate total amount
      totalAmountAccumulator.add(amount)
    }

    logger.info("Transaction processing completed!")
  }

  private def verifyWithSQL(transactions: DataFrame, threshold: Double): Unit = {
    logger.info("\n" + "=" * 80)
    logger.info("VERIFICATION WITH SQL AGGREGATION")
    logger.info("=" * 80)

    val stats = transactions.agg(
      count(when(col("amount") > threshold, 1)).as("high_value_count"),
      count(when(col("amount") <= threshold, 1)).as("low_value_count"),
      sum("amount").as("total_amount")
    ).collect()(0)

    logger.info(s"High-Value Count (SQL): ${stats.getLong(0)}")
    logger.info(s"Low-Value Count (SQL): ${stats.getLong(1)}")
    logger.info(s"Total Amount (SQL): ${stats.getDouble(2)}")
    logger.info("=" * 80)
  }
}