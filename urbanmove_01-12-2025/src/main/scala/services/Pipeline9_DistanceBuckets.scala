package services

import org.apache.spark.sql.{SparkSession, SaveMode}
import java.util.Properties

object Pipeline9_DistanceBuckets {

  def main(args: Array[String]): Unit = {

    val inputPath = "urbanmove_trips.csv"

    // Load DB properties from file
    val props = new Properties()
    props.load(getClass.getClassLoader.getResourceAsStream("db.properties"))

    val dbUrl = props.getProperty("db.url")
    val dbUser = props.getProperty("db.user")
    val dbPass = props.getProperty("db.password")
    val dbTable = props.getProperty("db.table")

    // Start Spark
    val spark = SparkSession.builder()
      .appName("Pipeline9")
      .master("local[*]")
      .getOrCreate()

    val sc = spark.sparkContext
    import spark.implicits._

    // Read CSV into RDD
    val rdd = sc.textFile(inputPath)
    val header = rdd.first()

    // Compute buckets
    val resultRDD = rdd
      .filter(_ != header)
      .map(_.split(",", -1))
      .filter(cols => cols.length >= 8)
      .flatMap(cols => scala.util.Try(cols(7).toDouble).toOption)
      .map { dist =>
        val bucket =
          if (dist < 5) "<5"
          else if (dist <= 10) "5-10"
          else ">10"
        (bucket, 1)
      }
      .reduceByKey(_ + _)

    // Convert to DataFrame
    val df = resultRDD.toDF("bucket", "count")

    // Create table if not exists
    val conn = java.sql.DriverManager.getConnection(dbUrl, dbUser, dbPass)
    val stmt = conn.createStatement()
    stmt.execute(
      """
        |CREATE TABLE IF NOT EXISTS distance_buckets (
        |  bucket VARCHAR(10),
        |  count INT
        |);
        |""".stripMargin)
    conn.close()

    // Save to MySQL
    df.write
      .format("jdbc")
      .option("url", dbUrl)
      .option("dbtable", dbTable)
      .option("user", dbUser)
      .option("password", dbPass)
      .mode(SaveMode.Append)
      .save()

    println("Data inserted to MySQL successfully")

    spark.stop()
  }
}
