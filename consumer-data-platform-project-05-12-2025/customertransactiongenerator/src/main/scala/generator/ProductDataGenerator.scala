package generator

import scala.util.Random
import models.Product
import config.DataGenerationConfig
import com.typesafe.scalalogging.LazyLogging

class ProductDataGenerator(config: DataGenerationConfig) extends LazyLogging {
  private val count = config.productCount
  private val categories = Seq(
    "Electronics", "Fashion", "Books", "Home", "Sports",
    "Toys", "Beauty", "Grocery", "Automotive", "Health"
  )
  private val priceMin = config.priceMin
  private val priceMax = config.priceMax

  def generate(): Seq[Product] = {
    logger.info(s"Generating products count $count")

    val startTime = System.currentTimeMillis()

    val products = (1 to count).map { id =>
      if (id % 50 == 0) logger.debug(s"Generated product index $id")

      val price = priceMin + BigDecimal(Random.nextDouble()) * (priceMax - priceMin)
      Product(
        product_id = id,
        name = s"Product_$id",
        category = categories(Random.nextInt(categories.length)),
        price = price.setScale(2, BigDecimal.RoundingMode.HALF_UP)
      )
    }

    val duration = System.currentTimeMillis() - startTime
    logger.info(s"Product generation completed count $count duration milliseconds $duration")
    products
  }
}