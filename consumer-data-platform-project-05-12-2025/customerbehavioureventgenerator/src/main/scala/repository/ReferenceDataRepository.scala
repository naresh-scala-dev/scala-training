package repository

import com.typesafe.scalalogging.LazyLogging
import config.AppConfig

class ReferenceDataRepository extends LazyLogging {

  Class.forName("com.mysql.cj.jdbc.Driver")
  private val dbUrl = AppConfig.mysql.url
  private val dbUser = AppConfig.mysql.user
  private val dbPassword = AppConfig.mysql.password

  def loadCustomerIds(): Seq[Int] = {
    logger.info("Loading customer_ids from MySQL")
    val conn = java.sql.DriverManager.getConnection(dbUrl, dbUser, dbPassword)
    val customers = scala.collection.mutable.ArrayBuffer[Int]()

    try {
      val rs = conn.createStatement().executeQuery("SELECT customer_id FROM customers ORDER BY customer_id")
      while (rs.next()) {
        customers += rs.getInt("customer_id")
      }
      rs.close()
      logger.info(s"Loaded ${customers.size} customer IDs")
    } catch {
      case e: Exception =>
        logger.error(s"Error loading customer IDs: ${e.getMessage}", e)
    } finally {
      conn.close()
    }

    customers.toSeq
  }

  def loadProductIds(): Seq[Int] = {
    logger.info("Loading product_ids from MySQL")
    val conn = java.sql.DriverManager.getConnection(dbUrl, dbUser, dbPassword)
    val products = scala.collection.mutable.ArrayBuffer[Int]()

    try {
      val rs = conn.createStatement().executeQuery("SELECT product_id FROM products ORDER BY product_id")
      while (rs.next()) {
        products += rs.getInt("product_id")
      }
      rs.close()
      logger.info(s"Loaded ${products.size} product IDs")
    } catch {
      case e: Exception =>
        logger.error(s"Error loading product IDs: ${e.getMessage}", e)
    } finally {
      conn.close()
    }

    products.toSeq
  }
}