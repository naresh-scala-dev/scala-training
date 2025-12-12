import extractor.MySQLExtractor
import transformer.TransactionSummarizer
import loader.ParquetLoader
import util.SparkSessionFactory
import com.typesafe.scalalogging.LazyLogging
import java.time.LocalDate

object TransactionSummaryJob extends LazyLogging {

  def main(args: Array[String]): Unit = {
    logger.info("╔════════════════════════════════════════════════════════════╗")
    logger.info("║        PIPELINE B: DAILY TRANSACTION SUMMARY               ║")
    logger.info("║        MySQL → Parquet (Batch, Scheduled Daily)            ║")
    logger.info("╚════════════════════════════════════════════════════════════╝")

    val spark = SparkSessionFactory.getOrCreateSession()

    try {
      // PRODUCTION LOGIC: Process previous day only (24 hours before)
      val targetDate = LocalDate.now().minusDays(1)

      logger.info("═══════════════════════════════════════════════════════════")
      logger.info(s"Processing: $targetDate (previous day)")
      logger.info("═══════════════════════════════════════════════════════════")

      val extractor = new MySQLExtractor(spark)
      val summarizer = new TransactionSummarizer
      val loader = new ParquetLoader

      // Step 1: Extract
      logger.info("Step 1: EXTRACT - Reading from MySQL")
      val transactionsDF = extractor.extractTransactions(
        targetDate = Some(targetDate),
        fullLoad = false
      )

      val extractedCount = transactionsDF.count()

      if (extractedCount == 0) {
        logger.warn(s"⚠️  No transactions found for $targetDate")
        logger.info("Pipeline B completed with no data to process")
        return
      }

      logger.info(s"✓ Extracted $extractedCount transactions")

      // Step 2: Transform
      logger.info("Step 2: TRANSFORM - Building daily transaction summaries")
      val summaryDF = summarizer.buildDailySummary(transactionsDF)
      logger.info(s"✓ Generated summaries")

      // Step 3: Load
      logger.info("Step 3: LOAD - Writing to Parquet")
      loader.write(summaryDF)
      logger.info(s"✓ Written to Parquet (date=$targetDate)")

      // Step 4: Validate
      logger.info("Step 4: VALIDATE - Reading back written Parquet")
      loader.validateOutput(spark)

      logger.info("╔════════════════════════════════════════════════════════════╗")
      logger.info("║         ✓ PIPELINE B COMPLETED SUCCESSFULLY                ║")
      logger.info(s"║  Date processed: $targetDate                              ║")
      logger.info("╚════════════════════════════════════════════════════════════╝")

    } catch {
      case e: Exception =>
        logger.error("╔════════════════════════════════════════════════════════════╗")
        logger.error("║           ✗ PIPELINE B FAILED                              ║")
        logger.error("╚════════════════════════════════════════════════════════════╝")
        logger.error(s"Error: ${e.getMessage}", e)
        System.exit(1)
    } finally {
      spark.stop()
      logger.info("Spark session closed")
    }
  }
}