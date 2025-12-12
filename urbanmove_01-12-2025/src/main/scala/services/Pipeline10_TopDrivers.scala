package services

import org.apache.commons.io.FileUtils
import org.apache.spark.sql.SparkSession
import java.nio.file.{Files, Paths}

object Pipeline10_TopDrivers {

  def main(args: Array[String]): Unit = {

    val inputPath  = if (args.length > 0) args(0) else "urbanmove_trips.csv"
    val outputPath = if (args.length > 1) args(1) else "output/pipeline10"

    val spark = SparkSession.builder()
      .appName("Pipeline10")
      .master("local[*]")
      .getOrCreate()

    val sc = spark.sparkContext

    // Delete existing output folder if exists
    val path = Paths.get(outputPath)
    if (Files.exists(path)) {
      FileUtils.deleteDirectory(path.toFile)
      println(s"Deleted existing folder: $outputPath")
    }

    val data = sc.textFile(inputPath)

    // Extract header
    val header = data.first()

    val topDrivers = data
      .filter(_ != header)
      .map(_.split(",", -1))
      .flatMap { cols =>
        if (cols.length >= 11 && isDouble(cols(8))) {
          Some((cols(1), cols(8).toDouble))
        } else None
      }
      .reduceByKey(_ + _)
      .sortBy(_._2, ascending = false)
      .take(10)
      .map { case (driver, fare) => s"$driver,$fare" }

    sc.parallelize(topDrivers).saveAsTextFile(outputPath)

    spark.stop()
  }

  // Helper: safe number check
  def isDouble(value: String): Boolean =
    value != null && value.nonEmpty && value.matches("""[-+]?\d*\.?\d+""")
}
