package services

import cache.CacheService
import com.typesafe.scalalogging.LazyLogging
import models.{DailySummary, AggregatedDailySummary}
import play.api.libs.json.Json
import repository.ParquetRepository
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SummaryService @Inject()(
                                parquetRepo: ParquetRepository,
                                cacheService: CacheService
                              )(implicit ec: ExecutionContext) extends LazyLogging {

  logger.info("SummaryService initialized")

  def getDailySummary(date: String, customerId: Int): Future[Option[DailySummary]] = {
    val startTime = System.currentTimeMillis()
    val cacheKey = s"$date:$customerId"
    cacheService.getSummary(cacheKey) match {
      case Some(cached) =>
        val elapsed = System.currentTimeMillis() - startTime
        logger.debug(s"Cache HIT: summary=$cacheKey (${elapsed}ms)")
        try {
          Future.successful(Some(Json.parse(cached).as[DailySummary]))
        } catch {
          case _: Exception => fetchAndCache(date, customerId, cacheKey, "cache_parse_error")
        }
      case None =>
        logger.debug(s"Cache MISS: summary=$cacheKey")
        fetchAndCache(date, customerId, cacheKey, "cache_miss")
    }
  }

  def getAllDailySummaries(date: String): Future[Option[AggregatedDailySummary]] = {
    val startTime = System.currentTimeMillis()
    val cacheKey = s"$date:ALL"
    cacheService.getSummary(cacheKey) match {
      case Some(cached) =>
        val elapsed = System.currentTimeMillis() - startTime
        logger.debug(s"Cache HIT: all_summaries=$date (${elapsed}ms)")
        try {
          Future.successful(Some(Json.parse(cached).as[AggregatedDailySummary]))
        } catch {
          case _: Exception => fetchAndCacheAll(date, cacheKey, "cache_parse_error")
        }
      case None =>
        logger.debug(s"Cache MISS: all_summaries=$date")
        fetchAndCacheAll(date, cacheKey, "cache_miss")
    }
  }

  private def fetchAndCache(date: String, customerId: Int, key: String, reason: String): Future[Option[DailySummary]] = {
    val startTime = System.currentTimeMillis()
    parquetRepo.getDailySummary(date, customerId).map { summaryOpt =>
      val elapsed = System.currentTimeMillis() - startTime
      summaryOpt.foreach { summary =>
        cacheService.setSummary(key, Json.stringify(Json.toJson(summary)))
        logger.info(s"Parquet READ (reason=$reason): summary=$key (${elapsed}ms)")
      }
      summaryOpt
    }
  }

  private def fetchAndCacheAll(date: String, key: String, reason: String): Future[Option[AggregatedDailySummary]] = {
    val startTime = System.currentTimeMillis()
    parquetRepo.getAllDailySummaries(date).map { aggOpt =>
      val elapsed = System.currentTimeMillis() - startTime
      aggOpt.foreach { agg =>
        cacheService.setSummary(key, Json.stringify(Json.toJson(agg)))
        logger.info(s"Parquet READ (reason=$reason): all_summaries=$date (${elapsed}ms)")
      }
      aggOpt
    }
  }
}
