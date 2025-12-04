package pipeline1

import config.AppConfig
import org.apache.spark.sql.{DataFrame, SparkSession}

object Pipeline1_RDS_to_Keyspaces {

  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder()
      .appName("Pipeline1-RDS-to-Keyspaces")
      .master("local[*]")
      .config("spark.cassandra.connection.host", AppConfig.cassandra.host)
      .config("spark.cassandra.connection.port", AppConfig.cassandra.port)
      .config("spark.cassandra.connection.ssl.enabled", "true")
      .config("spark.cassandra.connection.ssl.trustStore.path", AppConfig.cassandra.truststorePath)
      .config("spark.cassandra.connection.ssl.trustStore.password", AppConfig.cassandra.truststorePassword)
      .config("spark.cassandra.auth.username", AppConfig.cassandra.username)
      .config("spark.cassandra.auth.password", AppConfig.cassandra.password)
      .getOrCreate()

    val jdbcUrl = AppConfig.mysql.url

    def readTable(table: String): DataFrame =
      spark.read
        .format("jdbc")
        .option("url", jdbcUrl)
        .option("dbtable", table)
        .option("user", AppConfig.mysql.user)
        .option("password", AppConfig.mysql.password)
        .option("driver", "com.mysql.cj.jdbc.Driver")
        .load()

    val customersDF  = readTable("customers")
    val ordersDF     = readTable("orders")
    val itemsDF      = readTable("order_items")

    val finalDF =
      customersDF
        .join(ordersDF, "customer_id")
        .join(itemsDF, "order_id")
        .select(
          customersDF("customer_id"),
          customersDF("name"),
          customersDF("email"),
          customersDF("city"),
          ordersDF("order_id"),
          ordersDF("order_date"),
          ordersDF("amount"),
          itemsDF("item_id"),
          itemsDF("product_name"),
          itemsDF("quantity")
        )

    finalDF.write
      .format("org.apache.spark.sql.cassandra")
      .option("keyspace", "retail")
      .option("table", "sales_data")
      .mode("append")
      .save()

    spark.stop()
  }
}
