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
    import spark.implicits._


    val outputDir = Paths.get(outputPath)
    if (Files.exists(outputDir)) {
      FileUtils.deleteDirectory(outputDir.toFile)
      println(s"Deleted existing output folder: $outputPath")
    }


    val rdd = sc.textFile(inputPath)
    val header = rdd.first()

    val filtered = rdd
      .filter(_ != header)
      .map(_.split(",", -1))
      .filter(cols => cols.length >= 9)
      .filter(cols => scala.util.Try(cols(8).toDouble).getOrElse(0.0) > 100)


    val df = filtered.map(cols =>
      (cols(0), cols(1), cols(2), cols(3), cols(4), cols(5), cols(6), cols(7), cols(8))
    ).toDF("tripId", "city", "vehicleType", "startTime", "endTime",
      "startLat", "startLong", "endLat", "fareAmount")


    df.write.mode("overwrite").parquet("output/pipeline7_parquet")


    df.rdd.map(_.mkString(",")).saveAsTextFile(outputPath)

    spark.stop()
  }
}
