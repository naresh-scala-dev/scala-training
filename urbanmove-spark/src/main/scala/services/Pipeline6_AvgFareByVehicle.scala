package services

import org.apache.commons.io.FileUtils
import org.apache.spark.sql.SparkSession

import java.nio.file.{Files, Paths}

object Pipeline6_AvgFareByVehicle {
  def main(args: Array[String]): Unit = {

    val inputPath  = if (args.length > 0) args(0) else "urbanmove_trips.csv"
    val outputPath = if (args.length > 1) args(1) else "output/pipeline6_avgFareByVehicle"

    val spark = SparkSession.builder()
      .appName("Pipeline6_AvgFareByVehicle")
      .master("local[*]") // change when running on cluster
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

    // Remove header safely (only first partition)
    val rows = raw.mapPartitionsWithIndex { (idx, iter) =>
      if (idx == 0 && iter.hasNext) {
        val first = iter.next()
        if (first.toLowerCase.contains("tripid") && first.toLowerCase.contains("fareamount"))
          iter // skip header
        else
          Iterator(first) ++ iter
      } else {
        iter
      }
    }

    // Parse fareAmount and vehicleType safely
    val parsed = rows.flatMap { line =>
      val cols = line.split(",", -1)
      if (cols.length >= 11) {
        try {
          val vehicleType = cols(2)
          val fareAmount = cols(8).toDouble
          Some((vehicleType, fareAmount))
        } catch {
          case _: NumberFormatException => None
        }
      } else None
    }

    // Compute total fare and count by vehicleType
    val totalsAndCounts = parsed
      .map { case (veh, fare) => (veh, (fare, 1)) }
      .reduceByKey { case ((fare1, count1), (fare2, count2)) =>
        (fare1 + fare2, count1 + count2)
      }

    // Compute average fare per vehicle
    val avgFareByVehicle = totalsAndCounts.map { case (veh, (totalFare, count)) =>
      f"$veh,${totalFare / count}%.2f"
    }

    // Optional: show a small sample
    println("Average fare by vehicle type:")
    avgFareByVehicle.collect().foreach(println)

    // Save output as text files (one folder per Spark convention)
    avgFareByVehicle.saveAsTextFile(outputPath)

    println(s"Saved output to: $outputPath")

    spark.stop()
  }
}
