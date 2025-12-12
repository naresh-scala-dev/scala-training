package validation

import models._
import com.typesafe.scalalogging.LazyLogging

object DataValidator extends LazyLogging {

  def validateCustomers(customers: Seq[Customer]): Boolean = {
    logger.info(s"Validating customers count ${customers.length}")

    var invalidCount = 0
    customers.foreach { c =>
      val valid = c.name.nonEmpty && c.email.nonEmpty && Seq("M", "F", "O").contains(c.gender)
      if (!valid) {
        invalidCount += 1
        if (invalidCount <= 10) logger.warn(s"Invalid customer id ${c.customer_id} name ${c.name} email ${c.email}")
      }
    }

    if (invalidCount > 0) {
      logger.warn(s"Customer validation found invalid records count $invalidCount")
    } else {
      logger.info(s"Customer validation passed all records are valid")
    }
    invalidCount == 0
  }

  def validateProducts(products: Seq[Product]): Boolean = {
    logger.info(s"Validating products count ${products.length}")

    var invalidCount = 0
    products.foreach { p =>
      val valid = p.name.nonEmpty && p.category.nonEmpty && p.price > 0
      if (!valid) {
        invalidCount += 1
        if (invalidCount <= 10) logger.warn(s"Invalid product id ${p.product_id} name ${p.name} price ${p.price}")
      }
    }

    if (invalidCount > 0) {
      logger.warn(s"Product validation found invalid records count $invalidCount")
    } else {
      logger.info(s"Product validation passed all records are valid")
    }
    invalidCount == 0
  }

  def validateTransactions(transactions: Seq[Transaction]): Boolean = {
    logger.info(s"Validating transactions count ${transactions.length}")

    var invalidCount = 0
    transactions.foreach { t =>
      val valid = t.qty > 0 && t.amount >= 0 && t.customer_id > 0 && t.product_id > 0
      if (!valid) {
        invalidCount += 1
        if (invalidCount <= 10) logger.warn(s"Invalid transaction id ${t.txn_id} qty ${t.qty} amount ${t.amount}")
      }
    }

    if (invalidCount > 0) {
      logger.warn(s"Transaction validation found invalid records count $invalidCount")
    } else {
      logger.info(s"Transaction validation passed all records are valid")
    }
    invalidCount == 0
  }
}