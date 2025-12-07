package LogsCoalesce

import config.AppConfig
import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

import java.io.File

object LogsCoalesce {

  val logger: Logger = Logger.getLogger(getClass.getName)

  def fileCount(path: String): Int =
    Option(new File(path).listFiles()).map(_.count(_.isFile)).getOrElse(0)

  def main(args: Array[String]): Unit = {

    Logger.getLogger("org").setLevel(Level.ERROR)
    Logger.getLogger("akka").setLevel(Level.ERROR)

    val spark = SparkSession.builder()
      .appName("Logs Coalesce")
      .master("local[*]")
      .getOrCreate()

    import spark.implicits._

    logger.info("Generating five million log records")

    val logsDF = spark.range(0, 5000000)
      .repartition(20)
      .withColumn("level", when($"id" % 4 === 0, "ERROR").otherwise("INFO"))
      .withColumn("message", concat(lit("Log "), $"id"))

    logger.info("Total records generated: " + logsDF.count())
    logger.info("Initial partitions: " + logsDF.rdd.getNumPartitions)

    logsDF.write.mode("overwrite").parquet(AppConfig.paths.logCoalesce_input)

    val filteredDF = logsDF.filter($"level" === "ERROR")

    logger.info("Records after filtering: " + filteredDF.count())
    logger.info("Partitions after filtering: " + filteredDF.rdd.getNumPartitions)

    val startBefore = System.currentTimeMillis()
    filteredDF.write.mode("overwrite").parquet(AppConfig.paths.logCoalesce_outputBefore)
    val endBefore = System.currentTimeMillis()
    val filesBefore = fileCount(AppConfig.paths.logCoalesce_outputBefore)

    val startAfter = System.currentTimeMillis()
    filteredDF.coalesce(4).write.mode("overwrite").parquet(AppConfig.paths.logCoalesce_outputAfter)
    val endAfter = System.currentTimeMillis()
    val filesAfter = fileCount(AppConfig.paths.logCoalesce_outputAfter)

    logger.info("File count before coalesce: " + filesBefore)
    logger.info("File count after coalesce: " + filesAfter)
    logger.info("Write time before coalesce: " + (endBefore - startBefore))
    logger.info("Write time after coalesce: " + (endAfter - startAfter))
    logger.info("Coalesce reduces partitions without shuffle")
    logger.info("Repartition uses shuffle and is more expensive")

    Thread.sleep(30000)

    spark.stop()
  }
}
