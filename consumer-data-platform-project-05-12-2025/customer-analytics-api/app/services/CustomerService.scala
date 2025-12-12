package services

import cache.CacheService
import com.typesafe.scalalogging.LazyLogging
import models.CustomerProfile
import play.api.libs.json.Json
import repository.CassandraRepository
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CustomerService @Inject()(
                                 cassandraRepo: CassandraRepository,
                                 cacheService: CacheService
                               )(implicit ec: ExecutionContext) extends LazyLogging {

  logger.info("CustomerService initialized")

  def getCustomerProfile(customerId: Int): Future[Option[CustomerProfile]] = {
    val startTime = System.currentTimeMillis()
    cacheService.getCustomer(customerId) match {
      case Some(cached) =>
        val elapsed = System.currentTimeMillis() - startTime
        logger.debug(s"Cache HIT: customer=$customerId (${elapsed}ms)")
        try {
          Future.successful(Some(Json.parse(cached).as[CustomerProfile]))
        } catch {
          case _: Exception => fetchAndCache(customerId, "cache_parse_error")
        }
      case None =>
        logger.debug(s"Cache MISS: customer=$customerId")
        fetchAndCache(customerId, "cache_miss")
    }
  }

  private def fetchAndCache(customerId: Int, reason: String): Future[Option[CustomerProfile]] = {
    val startTime = System.currentTimeMillis()
    cassandraRepo.getProfile(customerId).map { profileOpt =>
      val elapsed = System.currentTimeMillis() - startTime
      profileOpt.foreach { profile =>
        cacheService.setCustomer(customerId, Json.stringify(Json.toJson(profile)))
        logger.info(s"Cassandra READ (reason=$reason): customer=$customerId (${elapsed}ms)")
      }
      profileOpt
    }
  }
}