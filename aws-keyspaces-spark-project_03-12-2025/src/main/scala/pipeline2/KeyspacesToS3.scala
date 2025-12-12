package pipeline2

import config.AppConfig
import org.apache.spark.sql.SparkSession

object KeyspacesToS3 {

  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder()
      .appName("Keyspaces -> Parquet on S3")
      .master("local[*]")
      // Cassandra / Keyspaces config
      .config("spark.cassandra.connection.host", AppConfig.cassandra.host)
      .config("spark.cassandra.connection.port", AppConfig.cassandra.port)
      .config("spark.cassandra.connection.ssl.enabled", "true")
      .config("spark.cassandra.input.consistency.level", "LOCAL_QUORUM")
      .config("spark.cassandra.auth.username", AppConfig.cassandra.username)
      .config("spark.cassandra.auth.password", AppConfig.cassandra.password)
      .config("spark.cassandra.connection.ssl.trustStore.path", AppConfig.cassandra.truststorePath)
      .config("spark.cassandra.connection.ssl.trustStore.password", AppConfig.cassandra.truststorePassword)
      .getOrCreate()

    // ---------------- Hadoop S3A configuration
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

    // Read Keyspaces table
    val salesDF = spark.read
      .format("org.apache.spark.sql.cassandra")
      .option("keyspace", AppConfig.cassandra.keyspace)
      .option("table", AppConfig.cassandra.table)
      .load()

    // Select required columns
    val selectedDF = salesDF.select("customer_id", "order_id", "amount", "product_name", "quantity")

    // Write to S3 as partitioned Parquet
    selectedDF.write
      .partitionBy("customer_id")
      .mode("overwrite")
      .parquet(AppConfig.s3.parquetOutputPath)

    spark.stop()
  }
}
