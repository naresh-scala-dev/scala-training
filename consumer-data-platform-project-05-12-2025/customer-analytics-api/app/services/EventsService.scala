package services

import com.typesafe.scalalogging.LazyLogging
import models.{CustomerEvent, EventsPage}
import repository.ParquetRepository
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EventsService @Inject()(
                               parquetRepo: ParquetRepository
                             )(implicit ec: ExecutionContext) extends LazyLogging {

  // Get events by customer ID and date
  def getEventsByDate(customerId: Int, date: String): Future[Option[EventsPage]] = {
    parquetRepo.getEventsByDate(customerId, date).map {
      case Some(events) =>
        Some(EventsPage(
          customerId = customerId,
          eventDate = date,
          totalCount = events.length,
          events = events
        ))
      case None => None
    }
  }

  // Get recent N events for a customer across last 30 days
  def getRecentEvents(customerId: Int, limit: Int): Future[Option[EventsPage]] = {
    parquetRepo.getRecentEventsByCustomer(customerId, limit).map {
      case Some(events) =>
        Some(EventsPage(
          customerId = customerId,
          eventDate = "recent",
          totalCount = events.length,
          events = events
        ))
      case None => None
    }
  }

  // Get all events for a specific date
  def getAllEventsByDate(date: String): Future[Option[List[CustomerEvent]]] = {
    parquetRepo.getAllEventsByDate(date)
  }
}