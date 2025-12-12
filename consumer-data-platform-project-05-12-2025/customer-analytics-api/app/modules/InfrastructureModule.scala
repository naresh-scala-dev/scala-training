package modules

import com.google.inject.{AbstractModule, Provides, Singleton}
import com.typesafe.scalalogging.LazyLogging
import cache.CacheService
import repository.{CassandraRepository, ParquetRepository}
import services.{CustomerService, SummaryService, EventsService}
import play.api.Configuration
import scala.concurrent.ExecutionContext

class InfrastructureModule extends AbstractModule with LazyLogging {

  logger.info("Initializing Play Application Infrastructure Module")

  override def configure(): Unit = {
    logger.info("Requesting eager initialization of all singletons")
  }

  @Provides
  @Singleton
  def provideCacheService(): CacheService = {
    logger.info("Initializing CacheService")
    val cache = new CacheService()
    logger.info("CacheService initialized successfully")
    cache
  }

  @Provides
  @Singleton
  def provideCassandraRepository(implicit ec: ExecutionContext): CassandraRepository = {
    logger.info("Initializing CassandraRepository - connecting to Cassandra")
    val repo = new CassandraRepository()
    logger.info("CassandraRepository initialized and connected successfully")
    repo
  }

  @Provides
  @Singleton
  def provideParquetRepository(implicit ec: ExecutionContext): ParquetRepository = {
    logger.info("Initializing ParquetRepository - connecting to S3")
    val repo = new ParquetRepository()
    logger.info("ParquetRepository initialized and connected to S3 successfully")
    repo
  }

  @Provides
  @Singleton
  def provideCustomerService(
                              cassandraRepo: CassandraRepository,
                              cacheService: CacheService
                            )(implicit ec: ExecutionContext): CustomerService = {
    logger.info("Initializing CustomerService")
    val service = new CustomerService(cassandraRepo, cacheService)
    logger.info("CustomerService initialized successfully")
    service
  }

  @Provides
  @Singleton
  def provideSummaryService(
                             parquetRepo: ParquetRepository,
                             cacheService: CacheService
                           )(implicit ec: ExecutionContext): SummaryService = {
    logger.info("Initializing SummaryService")
    val service = new SummaryService(parquetRepo, cacheService)
    logger.info("SummaryService initialized successfully")
    service
  }

  @Provides
  @Singleton
  def provideEventsService(
                            parquetRepo: ParquetRepository
                          )(implicit ec: ExecutionContext): EventsService = {
    logger.info("Initializing EventsService")
    val service = new EventsService(parquetRepo)
    logger.info("EventsService initialized successfully")
    service
  }
}

