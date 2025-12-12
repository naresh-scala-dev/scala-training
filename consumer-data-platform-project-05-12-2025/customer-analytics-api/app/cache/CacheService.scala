package cache

import com.github.benmanes.caffeine.cache.Caffeine
import com.typesafe.scalalogging.LazyLogging
import config.AppConfig
import javax.inject.Singleton
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit

@Singleton
class CacheService extends LazyLogging {

  private val ttl = AppConfig.cache.ttlSeconds.seconds
  private val maxSize = AppConfig.cache.maxSize

  logger.info(s"Initializing CacheService: TTL=${ttl.toSeconds}s, MaxSize=$maxSize")

  val customerCache = Caffeine.newBuilder()
    .maximumSize(maxSize.toLong)
    .expireAfterWrite(ttl.toMillis, TimeUnit.MILLISECONDS)
    .recordStats()
    .build[Int, String]()

  val summaryCache = Caffeine.newBuilder()
    .maximumSize(maxSize.toLong)
    .expireAfterWrite(ttl.toMillis, TimeUnit.MILLISECONDS)
    .recordStats()
    .build[String, String]()

  val eventsCache = Caffeine.newBuilder()
    .maximumSize(maxSize.toLong)
    .expireAfterWrite(ttl.toMillis, TimeUnit.MILLISECONDS)
    .recordStats()
    .build[String, String]()

  logger.info("✓ CacheService initialized")

  def getCustomer(id: Int): Option[String] = Option(customerCache.getIfPresent(id))
  def setCustomer(id: Int, data: String): Unit = customerCache.put(id, data)
  def invalidateCustomer(id: Int): Unit = customerCache.invalidate(id)

  def getSummary(key: String): Option[String] = Option(summaryCache.getIfPresent(key))
  def setSummary(key: String, data: String): Unit = summaryCache.put(key, data)

  def getEvents(key: String): Option[String] = Option(eventsCache.getIfPresent(key))
  def setEvents(key: String, data: String): Unit = eventsCache.put(key, data)

  def clearAll(): Unit = {
    customerCache.invalidateAll()
    summaryCache.invalidateAll()
    eventsCache.invalidateAll()
  }

  def getStats: Map[String, Any] = Map(
    "customerCache" -> Map("size" -> customerCache.estimatedSize(), "hitRate" -> f"${customerCache.stats().hitRate()}%.2f%%"),
    "summaryCache" -> Map("size" -> summaryCache.estimatedSize(), "hitRate" -> f"${summaryCache.stats().hitRate()}%.2f%%"),
    "eventsCache" -> Map("size" -> eventsCache.estimatedSize(), "hitRate" -> f"${eventsCache.stats().hitRate()}%.2f%%")
  )
}
