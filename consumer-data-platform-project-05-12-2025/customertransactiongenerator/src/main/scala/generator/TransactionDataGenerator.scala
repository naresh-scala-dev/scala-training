package generator

import java.time.LocalDateTime
import java.sql.Timestamp
import scala.util.Random
import models.{Transaction, Customer, Product}
import config.DataGenerationConfig
import com.typesafe.scalalogging.LazyLogging

class TransactionDataGenerator(
                                config: DataGenerationConfig,
                                customers: Seq[Customer],
                                products: Seq[Product]
                              ) extends LazyLogging {

  private val txnCount = config.transactionCount
  private val qtyMin = config.qtyMin
  private val qtyMax = config.qtyMax

  private val startDate = LocalDateTime.parse(config.startDate + "T00:00:00")
  private val endDate = LocalDateTime.parse(config.endDate + "T23:59:59")

  private def randomDate(): Timestamp = {
    val seconds = java.time.Duration.between(startDate, endDate).getSeconds
    val dt = startDate.plusSeconds((Random.nextDouble() * seconds).toLong)
    Timestamp.valueOf(dt)
  }

  def generate(): Seq[Transaction] = {
    logger.info(s"Generating transactions count $txnCount")

    val startTime = System.currentTimeMillis()

    val transactions = (1 to txnCount).map { id =>
      if (id % 10000 == 0) logger.debug(s"Generated transaction index $id")

      val customer = customers(Random.nextInt(customers.size))
      val product = products(Random.nextInt(products.size))
      val qty = qtyMin + Random.nextInt(qtyMax - qtyMin + 1)

      Transaction(
        txn_id = id,
        customer_id = customer.customer_id,
        product_id = product.product_id,
        qty = qty,
        amount = (product.price * qty).setScale(2, BigDecimal.RoundingMode.HALF_UP),
        txn_timestamp = randomDate()
      )
    }

    val duration = System.currentTimeMillis() - startTime
    logger.info(s"Transaction generation completed count $txnCount duration milliseconds $duration")
    transactions
  }
}