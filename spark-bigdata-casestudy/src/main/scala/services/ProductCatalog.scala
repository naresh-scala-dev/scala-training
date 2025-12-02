package services

import utils.{SparkUtil, Timer}
import scala.util.Random
import org.apache.log4j.{Level, Logger}

object ProductCatalog {
  val log: Logger = Logger.getLogger(getClass.getName)
  Logger.getLogger("org").setLevel(Level.WARN)

  def main(args: Array[String]): Unit = {
    val spark = SparkUtil.getSpark("ProductCatalog")
    import spark.implicits._

    Timer.time {
      val numProducts = 2000000
      val categories = Array("Electronics", "Clothes", "Books")

      val productRDD = spark.sparkContext.parallelize(1 to numProducts, 40)
        .map { id =>
          val cat = categories(Random.nextInt(categories.length))
          val price = Random.nextDouble() * 2000
          val desc = Random.alphanumeric.take(50).mkString
          (id.toLong, cat, price, desc)
        }

      val productDF = productRDD.toDF("productId", "category", "price", "description")
      val filtered = productDF.filter($"price" > 1000)
      val sorted = filtered.orderBy($"price".desc)
      sorted.explain(true)

      Timer.time { sorted.write.mode("overwrite").csv("output/ex4/products_sorted_csv"); Thread.sleep(30000); log.info("CSV WRITE DONE.") }
      Timer.time { sorted.write.mode("overwrite").parquet("output/ex4/products_sorted_parquet"); Thread.sleep(30000); log.info("PARQUET WRITE DONE.") }

    }

    log.info("COMPLETED ProductCatalog")
  }
}
