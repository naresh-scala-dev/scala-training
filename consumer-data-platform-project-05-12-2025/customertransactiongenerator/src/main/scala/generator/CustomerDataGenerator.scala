package generator

import java.time.LocalDate
import scala.util.Random
import models.Customer
import config.DataGenerationConfig
import utils.DateUtils
import com.typesafe.scalalogging.LazyLogging

class CustomerDataGenerator(config: DataGenerationConfig) extends LazyLogging {
  private val count = config.customerCount
  private val startDate = LocalDate.parse(config.startDate)
  private val endDate = LocalDate.parse(config.endDate)

  private def randomDate(): LocalDate = DateUtils.randomDate(startDate, endDate)

  def generate(): Seq[Customer] = {
    logger.info(s"Generating customers count $count")

    val startTime = System.currentTimeMillis()

    val customers = (1 to count).map { id =>
      if (id % 500 == 0) logger.debug(s"Generated customer index $id")

      Customer(
        customer_id = id,
        name = s"Customer_$id",
        email = s"customer_$id@example.com",
        gender = Seq("M", "F", "O")(Random.nextInt(3)),
        signup_date = randomDate()
      )
    }

    val duration = System.currentTimeMillis() - startTime
    logger.info(s"Customer generation completed count $count duration milliseconds $duration")
    customers
  }
}