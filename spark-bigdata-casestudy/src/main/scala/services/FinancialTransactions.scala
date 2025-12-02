package services

import utils.{SparkUtil, Timer}
import scala.util.Random
import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.functions._

object FinancialTransactions {
  val log: Logger = Logger.getLogger(getClass.getName)
  Logger.getLogger("org").setLevel(Level.WARN)

  def main(args: Array[String]): Unit = {
    val spark = SparkUtil.getSpark("FinancialTransactions")
    import spark.implicits._

    Timer.time {

      val numTxns = 3000000

      val txnRDD = spark.sparkContext.parallelize(1 to numTxns, 40)
        .map { _ =>
          val acc = "ACC_" + Random.nextInt(100000)
          val amt = Random.nextDouble() * 10000
          (acc, amt)
        }

      val txnDF = txnRDD.toDF("accountId", "amount")

      val top10RDD =
        txnRDD
          .reduceByKey(_ + _)
          .map(_.swap)
          .sortByKey(ascending = false)
          .map(_.swap)
          .take(10)

      log.info(s"Top10 RDD accounts: ${top10RDD.mkString(",")}")


      val top10DF =
        txnDF
          .groupBy("accountId")
          .sum("amount")
          .withColumnRenamed("sum(amount)", "total_amount")
          .orderBy($"total_amount".desc)
          .limit(10)

      top10DF.explain(true)
      top10DF.show(10, false)


      Timer.time {
        top10DF
          .write
          .mode("overwrite")
          .parquet("output/ex7/top_accounts_parquet")

        Thread.sleep(30000)
        log.info("PARQUET WRITE DONE.")
      }

    }

    log.info("COMPLETED FinancialTransactions")
    Thread.sleep(50000)
  }
}
