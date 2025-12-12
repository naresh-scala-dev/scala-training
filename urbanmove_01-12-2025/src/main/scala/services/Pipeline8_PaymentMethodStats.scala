package services

import org.apache.commons.io.FileUtils
import org.apache.spark.sql.SparkSession

import java.nio.file.{Files, Paths}

object Pipeline8_PaymentMethodStats {
  def main(args: Array[String]): Unit = {
    val inputPath  = if (args.length > 0) args(0) else "urbanmove_trips.csv"
    val outputPath = if (args.length > 1) args(1) else "output/pipeline8"

    val spark = SparkSession.builder().appName("Pipeline8").master("local[*]").getOrCreate()
    val sc = spark.sparkContext

    // Delete existing output folder if exists
    val path = Paths.get(outputPath)
    if (Files.exists(path)) {
      FileUtils.deleteDirectory(path.toFile)
      println(s"Deleted existing folder: $outputPath")
    }

    val rdd = sc.textFile(inputPath)
      .map(_.split(",", -1)(9)) // paymentMethod
      .map(method => (method, 1))
      .reduceByKey(_ + _)
      .map { case (method, count) => s"$method,$count" }

    rdd.saveAsTextFile(outputPath)
    spark.stop()
  }
}
