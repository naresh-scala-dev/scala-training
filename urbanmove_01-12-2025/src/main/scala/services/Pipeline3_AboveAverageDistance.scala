package services

import org.apache.commons.io.FileUtils
import org.apache.spark.sql.SparkSession

import java.nio.file.{Files, Paths}

object Pipeline3_AboveAverageDistance {
  def main(args: Array[String]): Unit = {
    val inputPath  = if (args.length > 0) args(0) else "output/pipeline2"
    val outputPath = if (args.length > 1) args(1) else "output/pipeline3"

    val spark = SparkSession.builder().appName("Pipeline3").master("local[*]").getOrCreate()
    val sc = spark.sparkContext
    // Delete existing output folder if exists
    val path = Paths.get(outputPath)
    if (Files.exists(path)) {
      FileUtils.deleteDirectory(path.toFile)
      println(s"Deleted existing folder: $outputPath")
    }


    val rdd = sc.textFile(inputPath)
      .map(line => {
        val parts = line.split(",")
        (parts(0), parts(1).toDouble)
      })

    val avgDist = rdd.map(_._2).mean()
    val filtered = rdd.filter(_._2 > avgDist).map { case (veh, dist) => f"$veh,$dist%.2f" }

    filtered.saveAsTextFile(outputPath)
    spark.stop()
  }
}
