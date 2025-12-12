package services

import utils.{SparkUtil, Timer}
import scala.util.Random
import org.apache.log4j.{Level, Logger}

object EmployeeData {
  val log: Logger = Logger.getLogger(getClass.getName)
  Logger.getLogger("org").setLevel(Level.WARN)

  def main(args: Array[String]): Unit = {
    val spark = SparkUtil.getSpark("EmployeeData")
    import spark.implicits._

    Timer.time {
      val numEmp = 1000000
      val depts = Array("HR", "IT", "Sales", "Finance")

      val empRDD = spark.sparkContext.parallelize(1 to numEmp, 20)
        .map { id =>
          val name = Random.alphanumeric.take(7).mkString
          val dept = depts(Random.nextInt(depts.length))
          val salary = 30000 + Random.nextInt(70000)
          (id, name, dept, salary)
        }

      val empDF = empRDD.toDF("empId", "name", "department", "salary")
      val avgSalary = empDF.groupBy("department").avg("salary")
      avgSalary.explain(true)


      Timer.time {
        avgSalary.write.mode("overwrite").csv("output/ex8/avg_salary_csv")
        log.info("CSV WRITE DONE.")
      }

    }

    log.info("COMPLETED EmployeeData")
    Thread.sleep(600000)
  }
}
