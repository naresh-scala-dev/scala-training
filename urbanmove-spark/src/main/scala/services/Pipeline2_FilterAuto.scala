package services

import org.apache.commons.io.FileUtils
import org.apache.spark.sql.SparkSession

import java.nio.file.{Files, Paths}

object Pipeline2_FilterAuto {
  def main(args: Array[String]): Unit = {
    val inputPath  = if (args.length > 0) args(0) else "output/pipeline1"
    val outputPath = if (args.length > 1) args(1) else "output/pipeline2"

    val spark = SparkSession.builder().appName("Pipeline2").master("local[*]").getOrCreate()
    val sc = spark.sparkContext

    // Delete existing output folder if exists
    val path = Paths.get(outputPath)
    if (Files.exists(path)) {
      FileUtils.deleteDirectory(path.toFile)
      println(s"Deleted existing folder: $outputPath")
    }

    val rdd = sc.textFile(inputPath)
    val filtered = rdd.filter(line => line.startsWith("AUTO,"))
    filtered.saveAsTextFile(outputPath)

    spark.stop()
  }
}
