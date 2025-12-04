package pipeline3

import config.AppConfig
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

object ParquetToJSON {

  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder()
      .appName("Parquet -> Aggregated JSON on S3")
      .master("local[*]")
      .getOrCreate()


    val hconf = spark.sparkContext.hadoopConfiguration

    hconf.set("fs.s3a.endpoint", AppConfig.s3.endpoint)
    hconf.set("fs.s3a.region", AppConfig.s3.region)
    hconf.set("fs.s3a.impl", AppConfig.s3.impl)
    hconf.set("fs.s3a.connection.maximum", "100")
    hconf.set("fs.s3a.path.style.access", AppConfig.s3.pathStyleAccess.toString)
    hconf.set("fs.s3a.fast.upload", "true")
    hconf.set("fs.s3a.access.key", AppConfig.s3.accessKey)
    hconf.set("fs.s3a.secret.key", AppConfig.s3.secretKey)
    hconf.set("fs.s3a.aws.credentials.provider", AppConfig.s3.credentialsProvider)

    // ---------------- Read Parquet from S3
    val salesDF = spark.read.parquet(AppConfig.s3.parquetOutputPath)

    // ---------------- Aggregate total quantity and revenue per product
    val aggDF = salesDF.groupBy("product_name")
      .agg(
        sum("quantity").alias("total_quantity"),
        sum("amount").alias("total_revenue")
      )

    // ---------------- Write aggregated JSON to S3
    aggDF.write
      .mode("overwrite")
      .json(AppConfig.s3.jsonOutputPath)

    spark.stop()
  }
}
