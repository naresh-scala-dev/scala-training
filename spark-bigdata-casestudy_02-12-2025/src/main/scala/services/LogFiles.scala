package services

import utils.{SparkUtil, Timer}
import scala.util.Random
import org.apache.log4j.{Level, Logger}

object LogFiles {
  val log: Logger = Logger.getLogger(getClass.getName)
  Logger.getLogger("org").setLevel(Level.WARN)

  def main(args: Array[String]): Unit = {
    val spark = SparkUtil.getSpark("LogFiles")
    import spark.implicits._

    Timer.time {
      val levels = Array("INFO", "WARN", "ERROR")
      val numLogs = 5000000

      val logsRDD = spark.sparkContext.parallelize(1 to numLogs, 40)
        .map { _ =>
          val ts = System.currentTimeMillis() - Random.nextInt(10000000)
          val level = levels(Random.nextInt(levels.length))
          val msg = Random.alphanumeric.take(15).mkString
          val user = Random.nextInt(10000)
          s"$ts|$level|$msg|$user"
        }

      val errorRDD = logsRDD.filter(_.contains("|ERROR|"))
      log.info(s"RDD ERROR sample: ${errorRDD.take(5).mkString(",")}")

      val logsDF = logsRDD.map(_.split("\\|")).map(a => (a(0), a(1), a(2), a(3))).toDF("timestamp", "level", "message", "userId")
      val errorDF = logsDF.filter($"level" === "ERROR")
      errorDF.explain(true)

      Timer.time { errorRDD.saveAsTextFile("output/ex3/error_logs_txt"); Thread.sleep(30000); log.info("ERROR TXT WRITE DONE.") }
      Timer.time { logsDF.write.mode("overwrite").json("output/ex3/full_logs_json"); Thread.sleep(30000); log.info("FULL JSON WRITE DONE.") }

    }

    log.info("COMPLETED LogFiles")
    Thread.sleep(30000)
  }
}
