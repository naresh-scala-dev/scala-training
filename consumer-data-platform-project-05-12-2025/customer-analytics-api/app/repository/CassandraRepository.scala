package repository

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.{PreparedStatement, Row}
import com.typesafe.scalalogging.LazyLogging
import config.AppConfig
import models.CustomerProfile
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future, blocking}
import java.net.InetSocketAddress
import java.sql.Timestamp

@Singleton
class CassandraRepository @Inject()(implicit ec: ExecutionContext) extends LazyLogging {

  // Lazy initialization
  private lazy val session: CqlSession = {
    val startTime = System.currentTimeMillis()
    logger.info(s"Initializing Cassandra session: ${AppConfig.cassandra.host}:${AppConfig.cassandra.port}")
    val builder = CqlSession.builder()
      .addContactPoint(new InetSocketAddress(AppConfig.cassandra.host, AppConfig.cassandra.port))
      .withLocalDatacenter(AppConfig.cassandra.datacenter)
      .withAuthCredentials(AppConfig.cassandra.username, AppConfig.cassandra.password)

    if (AppConfig.cassandra.sslEnabled) {
      System.setProperty("javax.net.ssl.trustStore", AppConfig.cassandra.truststorePath)
      System.setProperty("javax.net.ssl.trustStorePassword", AppConfig.cassandra.truststorePassword)
      builder.withSslContext(javax.net.ssl.SSLContext.getDefault)
      logger.info("SSL/TLS enabled for Cassandra connection")
    }

    val sess = builder.build()
    val duration = System.currentTimeMillis() - startTime
    logger.info(s"Cassandra session initialized in ${duration}ms")
    sess
  }

  private lazy val getProfileStmt: PreparedStatement = {
    val query =
      s"""
        SELECT customer_id, name, email, gender, total_spend, total_transactions,
               avg_order_value, first_purchase, last_purchase, favorite_category
        FROM ${AppConfig.cassandra.keyspace}.customer_profile
        WHERE customer_id = ?
      """
    session.prepare(query)
  }

  // Force session initialization (call once after login)
  def initSession(): Unit = session

  def getProfile(customerId: Int): Future[Option[CustomerProfile]] = Future {
    blocking {
      val startTime = System.currentTimeMillis()
      try {
        val boundStmt = getProfileStmt.bind(Integer.valueOf(customerId))
        val resultSet = session.execute(boundStmt)
        val row = resultSet.one()
        val duration = System.currentTimeMillis() - startTime
        if (row != null) Some(parseCustomerProfile(row)) else None
      } catch {
        case ex: Exception =>
          logger.error(s"Error fetching customer=$customerId", ex)
          throw ex
      }
    }
  }

  private def parseCustomerProfile(row: Row): CustomerProfile = {
    CustomerProfile(
      customer_id = row.getInt("customer_id"),
      name = row.getString("name"),
      email = row.getString("email"),
      gender = row.getString("gender"),
      total_spend = BigDecimal(row.getBigDecimal("total_spend")),
      total_transactions = row.getInt("total_transactions"),
      avg_order_value = BigDecimal(row.getBigDecimal("avg_order_value")),
      first_purchase = Option(row.getInstant("first_purchase")).map(i => new Timestamp(i.toEpochMilli)),
      last_purchase = Option(row.getInstant("last_purchase")).map(i => new Timestamp(i.toEpochMilli)),
      favorite_category = row.getString("favorite_category")
    )
  }

  def close(): Unit = if (session != null && !session.isClosed) {
    session.close()
    logger.info("Cassandra session closed")
  }
}
