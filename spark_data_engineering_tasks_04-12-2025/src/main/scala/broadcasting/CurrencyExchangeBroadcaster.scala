package broadcasting

import config.AppConfig
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.slf4j.LoggerFactory

import java.io.File
import scala.util.Random

case class Transaction(transaction_id: Int, currency: String, amount: Double)

case class ExchangeRate(currency: String, rateToUSD: Double)

object CurrencyExchangeBroadcaster {

  private val logger = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("CurrencyExchangeBroadcaster")
      .master("local[*]")
      .config("spark.sql.autoBroadcastJoinThreshold", "-1") // Disable auto broadcast to control manually
      .getOrCreate()

    spark.sparkContext.setLogLevel("WARN")

    createSampleDataIfNotExists(spark)

    val transactions = loadTransactions(spark)
    val rates = loadExchangeRates(spark)

    // Collect exchange rates into a Map and broadcast
    logger.info("Creating broadcast variable for exchange rates...")
    val ratesMap: Map[String, Double] = rates.collect()
      .map(row => row.getString(0) -> row.getDouble(1))
      .toMap

    val broadcastRates: Broadcast[Map[String, Double]] =
      spark.sparkContext.broadcast(ratesMap)

    logger.info(s"Broadcasted exchange rates: ${broadcastRates.value}")

    // Convert using broadcast variable (NO SHUFFLE)
    val converted = convertToUSDWithBroadcast(spark, transactions, broadcastRates)

    logger.info("Conversion completed without shuffle!")
    converted.show(10)

    // Count per currency (SHUFFLE HERE - but only for aggregation)
    val currencyCounts = countPerCurrency(converted)

    logger.info("Currency counts:")
    currencyCounts.show()

    // Write output with single partition to avoid shuffle
    currencyCounts.coalesce(1)
      .write
      .mode("overwrite")
      .json(AppConfig.paths.output)

    logger.info(s"Output written to: ${AppConfig.paths.output}")
    Thread.sleep(50000)
    // Cleanup
    broadcastRates.unpersist()


    spark.stop()
  }

  private def createSampleDataIfNotExists(spark: SparkSession): Unit = {
    val transactionsPath = AppConfig.paths.transactions
    val ratesPath = AppConfig.paths.exchangeRates

    import spark.implicits._

    if (!new File(transactionsPath).exists()) {
      logger.info(s"Creating sample transactions at $transactionsPath")

      val currencies = Seq("USD", "EUR", "GBP", "JPY", "AUD")
      val transactions = (1 to 100).map { id =>
        Transaction(
          transaction_id = id,
          currency = currencies(Random.nextInt(currencies.length)),
          amount = Random.nextDouble() * 1000
        )
      }.toDS().toDF()

      transactions.write.mode("overwrite").parquet(transactionsPath)
    }

    if (!new File(ratesPath).exists()) {
      logger.info(s"Creating sample exchange rates at $ratesPath")

      val rates = Seq(
        ExchangeRate("USD", 1.0),
        ExchangeRate("EUR", 1.1),
        ExchangeRate("GBP", 1.3),
        ExchangeRate("JPY", 0.007),
        ExchangeRate("AUD", 0.65)
      ).toDS().toDF()

      rates.write.mode("overwrite").parquet(ratesPath)
    }
  }

  private def loadTransactions(spark: SparkSession): DataFrame =
    spark.read.format("parquet").load(AppConfig.paths.transactions)

  private def loadExchangeRates(spark: SparkSession): DataFrame =
    spark.read.format("parquet").load(AppConfig.paths.exchangeRates)

  /**
   * Convert transactions to USD using broadcast variable
   * NO SHUFFLE - Map-side transformation only
   */
  private def convertToUSDWithBroadcast(
                                         spark: SparkSession,
                                         transactions: DataFrame,
                                         broadcastRates: Broadcast[Map[String, Double]]
                                       ): DataFrame = {

    // UDF that uses broadcast variable (read-only, no shuffle)
    val convertAmount = udf((currency: String, amount: Double) => {
      val rate = broadcastRates.value.getOrElse(currency, 1.0)
      amount * rate
    })

    transactions
      .withColumn("amount_usd", convertAmount(col("currency"), col("amount")))
      .select("transaction_id", "currency", "amount", "amount_usd")
  }

  /**
   * Count transactions per currency
   * SHUFFLE HERE - Required for aggregation (groupBy)
   */
  private def countPerCurrency(df: DataFrame): DataFrame = {
    df.groupBy("currency")
      .agg(
        count("*").as("transaction_count"),
        sum("amount_usd").as("total_amount_usd"),
        avg("amount_usd").as("avg_amount_usd")
      )
      .orderBy(desc("transaction_count"))
  }
}