package services

import org.apache.commons.io.FileUtils
import org.apache.spark.sql.SparkSession

import java.nio.file.{Files, Paths}

object Pipeline4_RandomSample {
  def main(args: Array[String]): Unit = {
    val inputPath  = if (args.length > 0) args(0) else "output/pipeline3"
    val outputPath = if (args.length > 1) args(1) else "output/pipeline4"

    val spark = SparkSession.builder().appName("Pipeline4").master("local[*]").getOrCreate()
    val sc = spark.sparkContext
    // Delete existing output folder if exists
    val path = Paths.get(outputPath)
    if (Files.exists(path)) {
      FileUtils.deleteDirectory(path.toFile)
      println(s"Deleted existing folder: $outputPath")
    }


    val rdd = sc.textFile(inputPath)
    val sample = rdd.sample(false, 0.1)
    sample.saveAsTextFile(outputPath)

    spark.stop()
  }
}
