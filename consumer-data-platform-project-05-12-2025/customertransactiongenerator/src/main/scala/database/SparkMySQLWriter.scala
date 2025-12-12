package database

import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}
import java.util.Properties

class SparkMySQLWriter(spark: SparkSession, jdbcUrl: String, dbProps: Properties) {

  def write(df: DataFrame, table: String, partitions: Int = 10): Unit = {
    df.repartition(partitions)
      .write
      .mode(SaveMode.Append)
      .jdbc(jdbcUrl, table, dbProps)
  }
}
