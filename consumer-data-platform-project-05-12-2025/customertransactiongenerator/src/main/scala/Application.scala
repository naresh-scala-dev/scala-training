
import org.apache.spark.sql.SparkSession
import com.typesafe.scalalogging.LazyLogging
import config.ConfigLoader
import database.{SparkSessionFactory, DataWriter}
import generator.{CustomerDataGenerator, ProductDataGenerator, TransactionDataGenerator}
import validation.DataValidator

object Application extends App with LazyLogging {

  logger.info("========== APPLICATION START ==========")

  try {
    // Step 1: Load configuration
    logger.info("Step 1: Loading configuration")
    val appConfig = ConfigLoader.load()
    val genConfig = appConfig.generation
    logger.info(s"Configuration loaded application database ${appConfig.mysql.database}")

    // Step 2: Create Spark session
    logger.info("Step 2: Creating Spark session")
    val spark: SparkSession = SparkSessionFactory.getOrCreateSession(appConfig)
    import spark.implicits._
    logger.info("Spark session created successfully")

    // Step 3: Initialize DataWriter
    logger.info("Step 3: Initializing data writer")
    val dataWriter = new DataWriter(spark, appConfig)
    logger.info("Data writer initialized")

    // Step 4: Generate data
    logger.info("Step 4: Generating data")
    val customerSeq = new CustomerDataGenerator(genConfig).generate()
    val productSeq = new ProductDataGenerator(genConfig).generate()
    val transactionSeq = new TransactionDataGenerator(genConfig, customerSeq, productSeq).generate()
    logger.info(s"Data generation completed: customers ${customerSeq.length}, products ${productSeq.length}, transactions ${transactionSeq.length}")

    // Step 5: Validate data
    logger.info("Step 5: Validating data")
    val customersValid = DataValidator.validateCustomers(customerSeq)
    val productsValid = DataValidator.validateProducts(productSeq)
    val transactionsValid = DataValidator.validateTransactions(transactionSeq)

    if (!customersValid || !productsValid || !transactionsValid) {
      logger.error("Data validation failed, aborting execution")
      System.exit(1)
    }
    logger.info("Data validation completed successfully")

    // Step 6: Load customers to MySQL
    logger.info("Step 6: Loading customers to MySQL")
    val startCust = System.nanoTime()
    val customerDS = spark.createDataset(customerSeq)
    val customerDF = customerDS.toDF().coalesce(1) // ensures single batch
    val customersLoaded = dataWriter.writeToMySQL(customerDF, "customers")
    val endCust = System.nanoTime()
    logger.info(s"Customers loaded successfully, count: $customersLoaded, time taken ${(endCust - startCust)/1e9} seconds")

    // Step 7: Load products to MySQL
    logger.info("Step 7: Loading products to MySQL")
    val startProd = System.nanoTime()
    val productDS = spark.createDataset(productSeq)
    val productDF = productDS.toDF().coalesce(1)
    val productsLoaded = dataWriter.writeToMySQL(productDF, "products")
    val endProd = System.nanoTime()
    logger.info(s"Products loaded successfully, count: $productsLoaded, time taken ${(endProd - startProd)/1e9} seconds")

    // Step 8: Load transactions to MySQL
    logger.info("Step 8: Loading transactions to MySQL")
    val startTrans = System.nanoTime()
    val transactionDS = spark.createDataset(transactionSeq)
    val transactionDF = transactionDS.toDF().coalesce(1) // single batch to avoid FK errors
    val transactionsLoaded = dataWriter.writeToMySQL(transactionDF, "transactions")
    val endTrans = System.nanoTime()
    logger.info(s"Transactions loaded successfully, count: $transactionsLoaded, time taken ${(endTrans - startTrans)/1e9} seconds")

    // Step 9: Verify data in database
    logger.info("Step 9: Verifying data in database")
    val customerCountDB = dataWriter.verifyTableCount("customers")
    val productCountDB = dataWriter.verifyTableCount("products")
    val transactionCountDB = dataWriter.verifyTableCount("transactions")
    logger.info(s"Database verification completed: customers $customerCountDB, products $productCountDB, transactions $transactionCountDB")

    logger.info("========== APPLICATION COMPLETED SUCCESSFULLY ==========")
    System.exit(0)

  } catch {
    case ex: Exception =>
      logger.error("Fatal error occurred in application execution", ex)
      logger.error(s"Error message: ${ex.getMessage}")
      System.exit(1)
  } finally {
    logger.info("Cleaning up resources")
    SparkSessionFactory.stopSession()
    logger.info("Application shutdown complete")
  }
}
