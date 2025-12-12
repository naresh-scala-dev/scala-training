package services

import org.apache.commons.io.FileUtils
import org.apache.spark.sql.SparkSession

import java.nio.file.{Files, Paths}

object Pipeline5_VehicleCount {
  def main(args: Array[String]): Unit = {
    val inputPath  = if (args.length > 0) args(0) else "output/pipeline4"
    val outputPath = if (args.length > 1) args(1) else "output/pipeline5"

    val spark = SparkSession.builder().appName("Pipeline5").master("local[*]").getOrCreate()
    val sc = spark.sparkContext

    // Delete existing output folder if exists
    val path = Paths.get(outputPath)
    if (Files.exists(path)) {
      FileUtils.deleteDirectory(path.toFile)
      println(s"Deleted existing folder: $outputPath")
    }

    val rdd = sc.textFile(inputPath)
      .map(line => (line.split(",")(0), 1))
      .reduceByKey(_ + _)
      .map { case (veh, count) => s"$veh,$count" }

    rdd.saveAsTextFile(outputPath)
    spark.stop()
  }
}
