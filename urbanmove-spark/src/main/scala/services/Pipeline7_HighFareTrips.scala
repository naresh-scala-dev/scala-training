package services

import org.apache.commons.io.FileUtils
import org.apache.spark.sql.SparkSession
import java.nio.file.{Files, Paths}

object Pipeline7_HighFareTrips {

  def main(args: Array[String]): Unit = {

    val inputPath  = if (args.length > 0) args(0) else "urbanmove_trips.csv"
    val outputPath = if (args.length > 1) args(1) else "output/pipeline7"

    val spark = SparkSession.builder()
      .appName("Pipeline7")
      .master("local[*]")
      .getOrCreate()

    val sc = spark.sparkContext

    // Delete old output dir
    val outputDir = Paths.get(outputPath)
    if (Files.exists(outputDir)) {
      FileUtils.deleteDirectory(outputDir.toFile)
      println(s"Deleted existing output folder: $outputPath")
    }

    // Load data
    val rdd = sc.textFile(inputPath)

    val header = rdd.first()         // get header row

    val result = rdd
      .filter(line => line != header)    // skip header
      .map(_.split(",", -1))
      .filter(cols => cols.length >= 9)
      .filter(cols =>
        scala.util.Try(cols(8).toDouble).getOrElse(0.0) > 100     // safe parse
      )
      .map(_.mkString(","))

    result.saveAsTextFile(outputPath)

    spark.stop()
  }
}
