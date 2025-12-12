import extractor.MySQLExtractor
import loader.CassandraLoader
import transformer.DataTransformer
import util.SparkSessionFactory
import com.typesafe.scalalogging.LazyLogging

object ProfileConsolidationJob extends LazyLogging {
  def main(args: Array[String]): Unit = {
    logger.info("Starting Pipeline A - Customer Profile Consolidation")

    val spark = SparkSessionFactory.getOrCreateSession()

    try {
      // Extraction
      val extractor = new MySQLExtractor()
      val customers = extractor.extractCustomers(spark)
      val products = extractor.extractProducts(spark)
      val transactions = extractor.extractTransactions(spark)

      // Transformation
      val transformer = new DataTransformer(spark)
      val profiles = transformer.transform(customers, transactions, products)

      // Load
      val loader = new CassandraLoader(spark)
      loader.initializeSchema()
      loader.loadProfiles(profiles)

      logger.info("Pipeline A completed successfully")
    } catch {
      case e: Exception =>
        logger.error("Pipeline A failed", e)
        System.exit(1)
    } finally {
      SparkSessionFactory.stopSession()
    }
  }
}
