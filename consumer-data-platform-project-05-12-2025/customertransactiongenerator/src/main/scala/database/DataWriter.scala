package database

import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}
import config.AppConfig
import com.typesafe.scalalogging.LazyLogging
import java.util.Properties

class DataWriter(spark: SparkSession, appConfig: AppConfig) extends LazyLogging {

  private val mysqlConfig = appConfig.mysql
  private val perfConfig = appConfig.performance

  private val properties = new Properties()
  properties.setProperty("user", mysqlConfig.username)
  properties.setProperty("password", mysqlConfig.password)
  properties.setProperty("driver", "com.mysql.cj.jdbc.Driver")
  properties.setProperty("batchsize", perfConfig.mysqlBatchInsertSize.toString)
  properties.setProperty("fetchSize", perfConfig.mysqlFetchSize.toString)
  properties.setProperty("connectionTimeout", perfConfig.mysqlConnectionTimeout.toString)

  def writeToMySQL(dataframe: DataFrame, tableName: String, mode: SaveMode = SaveMode.Append): Long = {
    logger.info(s"Starting write to MySQL table $tableName")

    val startTime = System.currentTimeMillis()
    val rowCount = dataframe.count()
    logger.debug(s"DataFrame row count for table $tableName is $rowCount")

    try {
      dataframe
        .repartition(perfConfig.partitions)
        .write
        .mode(mode)
        .jdbc(mysqlConfig.jdbcUrl, tableName, properties)

      val duration = System.currentTimeMillis() - startTime
      val throughput = (rowCount.toDouble / (duration / 1000.0)).toLong

      logger.info(s"Successfully written to MySQL table $tableName rows $rowCount duration milliseconds $duration throughput records per second $throughput")
      rowCount
    } catch {
      case ex: Exception =>
        logger.error(s"Failed to write to MySQL table $tableName", ex)
        throw ex
    }
  }

  def verifyTableCount(tableName: String): Long = {
    logger.info(s"Verifying row count in table $tableName")

    try {
      val countDF = spark.read.jdbc(mysqlConfig.jdbcUrl, tableName, properties)
      val count = countDF.count()
      logger.info(s"Table $tableName verification complete row count is $count")
      count
    } catch {
      case ex: Exception =>
        logger.error(s"Failed to verify table $tableName", ex)
        throw ex
    }
  }
}