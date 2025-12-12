package modules

import javax.inject.Inject
import com.typesafe.scalalogging.LazyLogging
import play.api.inject.ApplicationLifecycle
import cache.CacheService
import repository.{CassandraRepository, ParquetRepository}
import services.{CustomerService, SummaryService, EventsService}
import scala.concurrent.Future

class OnStartup @Inject()(
                           cacheService: CacheService,
                           cassandraRepository: CassandraRepository,
                           parquetRepository: ParquetRepository,
                           customerService: CustomerService,
                           summaryService: SummaryService,
                           eventsService: EventsService,
                           lifecycle: ApplicationLifecycle
                         ) extends LazyLogging {

  logger.info("Application Startup - All Services Ready")
  logger.info("CacheService initialized")
  logger.info("CassandraRepository connected")
  logger.info("ParquetRepository connected to S3")
  logger.info("CustomerService ready")
  logger.info("SummaryService ready")
  logger.info("EventsService ready")
  logger.info("Application Ready - Accepting Requests")

  // Register shutdown hook to close Cassandra connection
  lifecycle.addStopHook { () =>
    logger.info("Application shutting down - closing connections")
    try {
      cassandraRepository.close()
      logger.info("Cassandra connection closed successfully")
    } catch {
      case ex: Exception => logger.error("Error closing Cassandra connection", ex)
    }
    Future.successful(())
  }
}