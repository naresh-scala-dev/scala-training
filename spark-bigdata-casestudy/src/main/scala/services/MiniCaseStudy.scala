package services

import utils.{SparkUtil, Timer}
import scala.util.Random
import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.functions._

object MiniCaseStudy {
  val log: Logger = Logger.getLogger(getClass.getName)
  Logger.getLogger("org").setLevel(Level.WARN)

  def main(args: Array[String]): Unit = {
    val spark = SparkUtil.getSpark("MiniCaseStudy")
    import spark.implicits._

    Timer.time {
      val custCount = 2000000

      val custRDD = spark.sparkContext.parallelize(1 to custCount, 50)
        .map { id =>
          val name = Random.alphanumeric.take(8).mkString
          (id, name)
        }

      val custDF = custRDD.toDF("customerId", "name")

      val txnCount = 5000000

      val txnRDD2 = spark.sparkContext.parallelize(1 to txnCount, 80)
        .map { tid =>
          val cust = Random.nextInt(custCount) + 1
          val amt = Random.nextDouble() * 1000
          (tid, cust, amt)
        }

      val txnDF2 = txnRDD2.toDF("txnId", "customerId", "amount")

      val joined = custDF.join(txnDF2, "customerId")

      val totalPerCustomer =
        joined
          .groupBy("customerId")
          .sum("amount")
          .withColumnRenamed("sum(amount)", "total_amount")

      totalPerCustomer.explain(true)

      Timer.time {
        totalPerCustomer
          .write
          .mode("overwrite")
          .parquet("output/ex10/customer_spend_parquet")

        Thread.sleep(30000)
        log.info("PARQUET WRITE DONE.")
      }

    }

    log.info("COMPLETED MiniCaseStudy")
  }
}
