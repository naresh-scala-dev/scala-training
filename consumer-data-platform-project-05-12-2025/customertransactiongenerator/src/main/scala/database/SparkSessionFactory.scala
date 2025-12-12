package database

import org.apache.spark.sql.SparkSession
import config.{AppConfig, PerformanceConfig}
import com.typesafe.scalalogging.LazyLogging

object SparkSessionFactory extends LazyLogging {

  private var sparkSession: Option[SparkSession] = None

  def getOrCreateSession(appConfig: AppConfig): SparkSession = {
    sparkSession match {
      case Some(session) =>
        logger.debug("Returning existing Spark session")
        session
      case None =>
        logger.info("Creating new Spark session with performance tuning")
        val perfConfig = appConfig.performance

        val session = SparkSession.builder()
          .appName("CustomerTransactionGenerator")
          .master("local[*]")
          .config("spark.sql.shuffle.partitions", perfConfig.shufflePartitions)
          .config("spark.executor.cores", perfConfig.executorCores)
          .config("spark.executor.memory", perfConfig.executorMemory)
          .config("spark.driver.memory", perfConfig.driverMemory)
          .config("spark.shuffle.compress", true)
          .config("spark.io.compression.codec", "snappy")
          .config("spark.sql.adaptive.enabled", true)
          .config("spark.sql.adaptive.coalescePartitions.enabled", true)
          .config("spark.sql.adaptive.skewJoin.enabled", true)
          .config("spark.default.parallelism", perfConfig.partitions)
          .getOrCreate()

        session.sparkContext.setLogLevel("WARN")
        logger.info("Spark session created successfully with performance optimizations")
        sparkSession = Some(session)
        session
    }
  }

  def stopSession(): Unit = {
    sparkSession match {
      case Some(session) =>
        logger.info("Stopping Spark session")
        session.stop()
        sparkSession = None
        logger.info("Spark session stopped")
      case None =>
        logger.debug("No active Spark session to stop")
    }
  }
}