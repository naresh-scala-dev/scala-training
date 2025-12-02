package services

import utils.{SparkUtil, Timer}
import scala.util.Random
import org.apache.log4j.{Level, Logger}

object CustomersCityCount {
  val log: Logger = Logger.getLogger(getClass.getName)
  Logger.getLogger("org").setLevel(Level.WARN)

  def main(args: Array[String]): Unit = {
    val spark = SparkUtil.getSpark("CustomersCityCount")
    import spark.implicits._

    Timer.time {
      val numRecords = 5000000
      val cities = (1 to 50).map(i => s"City_$i").toArray

      val customersRDD = spark.sparkContext.parallelize(1 to numRecords, 50)
        .map { _ =>
          val city = cities(Random.nextInt(cities.length))
          (city, 1)
        }
      val rddCounts = customersRDD.reduceByKey(_ + _)
      log.info("RDD sample counts:")
      rddCounts.take(10).foreach(c => log.info(c.toString))

      val customersDF = spark.sparkContext.parallelize(1 to numRecords, 50)
        .map { id =>
          val name = Random.alphanumeric.take(10).mkString
          val age = 18 + Random.nextInt(53)
          val city = cities(Random.nextInt(cities.length))
          (id.toLong, name, age, city)
        }.toDF("customerId", "name", "age", "city")

      val dfCounts = customersDF.groupBy("city").count()
      dfCounts.explain(true)

      Timer.time {
        dfCounts.write.mode("overwrite").csv("output/ex1/customers_csv");
        log.info("CSV WRITE DONE.")
        Thread.sleep(30000)
      }
      Timer.time {
        dfCounts.write.mode("overwrite").json("output/ex1/customers_json");
        log.info("JSON WRITE DONE.")
        Thread.sleep(30000)
      }
      Timer.time {
        dfCounts.write.mode("overwrite").parquet("output/ex1/customers_parquet");
        log.info("PARQUET WRITE DONE.")
        Thread.sleep(30000)
      }

    }

    log.info("COMPLETED CustomerData")
  }
}
