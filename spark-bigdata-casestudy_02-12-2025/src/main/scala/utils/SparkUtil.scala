package utils

import org.apache.spark.sql.SparkSession

object SparkUtil {
  def getSpark(appName: String): SparkSession = {
    SparkSession.builder()
      .appName(appName)
      .master("local[*]")
      .config("spark.sql.shuffle.partitions", "100")
      .getOrCreate()
  }
}
