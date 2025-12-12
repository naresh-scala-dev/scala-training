package config

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

object ConfigLoader extends LazyLogging {
  private val conf = ConfigFactory.load()

  def load(): AppConfig = {
    logger.info("Loading application configuration from application.conf")

    val mysql = MySQLConfig(
      host = conf.getString("mysql.host"),
      port = conf.getInt("mysql.port"),
      database = conf.getString("mysql.database"),
      username = conf.getString("mysql.username"),
      password = conf.getString("mysql.password")
    )
    logger.debug(s"MySQL config loaded host ${mysql.host} database ${mysql.database}")

    val generation = DataGenerationConfig(
      customerCount = conf.getInt("generation.customer-count"),
      productCount = conf.getInt("generation.product-count"),
      transactionCount = conf.getInt("generation.transaction-count"),
      qtyMin = conf.getInt("generation.qty-min"),
      qtyMax = conf.getInt("generation.qty-max"),
      priceMin = BigDecimal(conf.getString("generation.price-min")),
      priceMax = BigDecimal(conf.getString("generation.price-max")),
      startDate = conf.getString("generation.start-date"),
      endDate = conf.getString("generation.end-date"),
      batchSize = conf.getInt("generation.batch-size")
    )
    logger.debug(s"Generation config loaded customers ${generation.customerCount} products ${generation.productCount} transactions ${generation.transactionCount}")

    val validation = ValidationConfig(
      enabled = conf.getBoolean("validation.enabled")
    )
    logger.debug(s"Validation config loaded enabled ${validation.enabled}")

    val performance = PerformanceConfig(
      bulkBatchSize = conf.getInt("performance.bulk-batch-size"),
      partitions = conf.getInt("performance.partitions"),
      shufflePartitions = conf.getInt("performance.shuffle-partitions"),
      executorCores = conf.getInt("performance.executor-cores"),
      executorMemory = conf.getString("performance.executor-memory"),
      driverMemory = conf.getString("performance.driver-memory"),
      mysqlBatchInsertSize = conf.getInt("performance.mysql-batch-insert-size"),
      mysqlFetchSize = conf.getInt("performance.mysql-fetch-size"),
      mysqlConnectionTimeout = conf.getInt("performance.mysql-connection-timeout")
    )
    logger.debug(s"Performance config loaded partitions ${performance.partitions} bulk-batch-size ${performance.bulkBatchSize}")

    val appConfig = AppConfig(mysql, generation, validation, performance)
    logger.info("Application configuration loaded successfully")
    appConfig
  }
}