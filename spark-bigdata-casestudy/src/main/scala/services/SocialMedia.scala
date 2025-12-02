package services

import utils.{SparkUtil, Timer}
import scala.util.Random
import org.apache.log4j.{Level, Logger}

object SocialMedia {
  val log: Logger = Logger.getLogger(getClass.getName)
  Logger.getLogger("org").setLevel(Level.WARN)

  def main(args: Array[String]): Unit = {
    val spark = SparkUtil.getSpark("SocialMedia")
    import spark.implicits._

    Timer.time {
      val userCount = 1000000
      val userRDD = spark.sparkContext.parallelize(1 to userCount, 30)
        .map { id =>
          val name = Random.alphanumeric.take(8).mkString
          val age = 15 + Random.nextInt(60)
          (id, name, age)
        }
      val userDF = userRDD.toDF("userId", "name", "age")

      val postCount = 2000000
      val postRDD = spark.sparkContext.parallelize(1 to postCount, 40)
        .map { pid =>
          val user = Random.nextInt(userCount) + 1
          val txt = Random.alphanumeric.take(20).mkString
          (pid, user, txt)
        }
      val postDF = postRDD.toDF("postId", "userId", "text")

      val joined = userDF.join(postDF, "userId")
      val postsPerAge = joined.groupBy("age").count()
      postsPerAge.explain(true)

      Timer.time { postsPerAge.write.mode("overwrite").json("output/ex6/posts_per_age_json"); Thread.sleep(30000); log.info("JSON WRITE DONE.") }

    }

    log.info("COMPLETED SocialMedia")
  }
}
