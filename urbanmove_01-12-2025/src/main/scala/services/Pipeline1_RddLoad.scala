package services

import org.apache.spark.sql.SparkSession
import java.nio.file.{Files, Paths}
import org.apache.commons.io.FileUtils

object Pipeline1_RddLoad {

  def main(args: Array[String]): Unit = {
    val inputPath  = if (args.length > 0) args(0) else "urbanmove_trips.csv"
    val outputPath = if (args.length > 1) args(1) else "output/pipeline1"
    val minDistance = 10.0

    val spark = SparkSession.builder()
      .appName("Pipeline1_RddLoad")
      .master("local[*]")
      .getOrCreate()

    val sc = spark.sparkContext

    // Delete existing output folder if exists
    val path = Paths.get(outputPath)
    if (Files.exists(path)) {
      FileUtils.deleteDirectory(path.toFile)
      println(s"Deleted existing folder: $outputPath")
    }

    // Read CSV as RDD[String]
    val raw = sc.textFile(inputPath)

    // Remove header safely: drop header only from the first partition
    val rows = raw.mapPartitionsWithIndex { (idx, iter) =>
      if (idx == 0 && iter.hasNext) {
        val first = iter.next()
        if (first.toLowerCase.contains("tripid") && first.toLowerCase.contains("starttime"))
          iter
        else
          Iterator(first) ++ iter
      } else {
        iter
      }
    }

    // Parse CSV lines into fields
    val parsed = rows.map(line => {
      val cols = line.split(",", -1)
      if (cols.length >= 11) {
        try {
          val vehicleType = cols(2)
          val distanceKm = cols(7).toDouble
          Some((vehicleType, distanceKm))
        } catch {
          case _: Throwable => None
        }
      } else None
    }).flatMap(x => x)

    // Filter trips with distance > minDistance
    val filtered = parsed.filter { case (_, dist) => dist > minDistance }

    // Map to CSV-like string
    val outputRdd = filtered.map { case (veh, dist) => f"$veh,${dist}%.2f" }

    println("Sample results (first 10):")
    outputRdd.take(10).foreach(println)

    val count = outputRdd.count()
    println(s"Total trips with distance > $minDistance km : $count")

    // Save output (overwrites folder if exists)
    outputRdd.saveAsTextFile(outputPath)
    println(s"Saved filtered output to: $outputPath")

    spark.stop()
  }
}
