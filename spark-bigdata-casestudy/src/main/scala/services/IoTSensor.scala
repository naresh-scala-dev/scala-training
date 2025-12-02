package services

import utils.{SparkUtil, Timer}
import scala.util.Random
import org.apache.log4j.{Level, Logger}

object IoTSensor {
  val log: Logger = Logger.getLogger(getClass.getName)
  Logger.getLogger("org").setLevel(Level.WARN)

  def main(args: Array[String]): Unit = {
    val spark = SparkUtil.getSpark("IoTSensor")
    import spark.implicits._

    Timer.time {

      val numSensors = 3000000

      val sensorRDD = spark.sparkContext.parallelize(1 to numSensors, 40)
        .map { _ =>
          val dev = "DEV_" + Random.nextInt(5000)
          val temp = 20 + Random.nextDouble() * 15
          val hum = 40 + Random.nextDouble() * 20
          val hour = Random.nextInt(24)
          (dev, temp, hum, hour)
        }

      val sensorDF = sensorRDD.toDF("deviceId", "temperature", "humidity", "hour")


      val avgTemp = sensorDF
        .groupBy("hour")
        .avg("temperature")
        .withColumnRenamed("avg(temperature)", "avg_temperature")

      avgTemp.explain(true)

      Timer.time {
        avgTemp.write
          .mode("overwrite")
          .partitionBy("hour")
          .parquet("output/ex5/avg_temp_by_hour")

        Thread.sleep(30000)
        log.info("PARQUET PARTITIONED WRITE DONE.")
      }

    }

    log.info("COMPLETED IoTSensor")
    Thread.sleep(30000)
  }
}
