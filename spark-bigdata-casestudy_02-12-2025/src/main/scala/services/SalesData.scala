package services

import utils.{SparkUtil, Timer}
import scala.util.Random
import org.apache.log4j.{Level, Logger}

object SalesData {
  val log: Logger = Logger.getLogger(getClass.getName)
  Logger.getLogger("org").setLevel(Level.WARN)

  def main(args: Array[String]): Unit = {
    val spark = SparkUtil.getSpark("SalesData")
    import spark.implicits._

    Timer.time {
      val numSales = 10000000
      val stores = (1 to 100).map(i => s"Store_$i").toArray


      val salesRDD = spark.sparkContext.parallelize(1 to numSales, 50)
        .map { _ =>
          val store = stores(Random.nextInt(stores.length))
          val amt = Random.nextDouble() * 500
          (store, amt)
        }

      val grouped = Timer.time { salesRDD.groupByKey().mapValues(_.sum).cache() }
      val reduced = Timer.time { salesRDD.reduceByKey(_ + _).cache() }

      log.info(s"RDD groupByKey sample: ${grouped.take(5).mkString(",")}")
      log.info(s"RDD reduceByKey sample: ${reduced.take(5).mkString(",")}")


      val salesDF = salesRDD.toDF("storeId", "amount")
      val dfAgg = salesDF.groupBy("storeId")
        .sum("amount")
        .withColumnRenamed("sum(amount)", "totalAmount") // <- rename column

      dfAgg.explain(true)


      Timer.time {
        dfAgg.write.mode("overwrite").parquet("output/ex2/sales_parquet")
        Thread.sleep(30000)
        log.info("PARQUET WRITE DONE.")
      }

    }

    log.info("COMPLETED SalesData")
    Thread.sleep(30000)
  }
}
