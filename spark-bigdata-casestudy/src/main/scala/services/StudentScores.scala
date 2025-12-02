package services

import utils.{SparkUtil, Timer}
import scala.util.Random
import org.apache.log4j.{Level, Logger}

object StudentScores {
  val log: Logger = Logger.getLogger(getClass.getName)
  Logger.getLogger("org").setLevel(Level.WARN)

  def main(args: Array[String]): Unit = {
    val spark = SparkUtil.getSpark("StudentScores")
    import spark.implicits._

    Timer.time {
      val numStudents = 1500000
      val studentRDD = spark.sparkContext.parallelize(1 to numStudents, 20)
        .map { id =>
          val name = Random.alphanumeric.take(6).mkString
          val score = Random.nextInt(100)
          (id, name, score)
        }

      val studentDF = studentRDD.toDF("studentId", "name", "score")
      val sorted = studentDF.orderBy($"score".desc)
      sorted.explain(true)

      Timer.time { sorted.write.mode("overwrite").json("output/ex9/students_sorted_json"); Thread.sleep(30000); log.info("JSON WRITE DONE.") }

    }

    log.info("COMPLETED StudentScores")
    Thread.sleep(40000)
  }
}
