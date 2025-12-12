package controllers

import play.api.mvc._
import play.api.libs.json.Json
import com.typesafe.scalalogging.LazyLogging
import services.EventsService
import models.{ApiResponse, CustomerEvent, EventsPage}
import security.AuthAction

import javax.inject._
import scala.concurrent.ExecutionContext

@Singleton
class EventsController @Inject()(
                                  cc: ControllerComponents,
                                  eventsService: EventsService,
                                  authAction: AuthAction
                                )(implicit ec: ExecutionContext) extends AbstractController(cc) with LazyLogging {

  implicit val eventWrites = CustomerEvent.customerEventFormat
  implicit val eventsPageWrites = EventsPage.eventsPageFormat

  def getEventsByDate(customerId: Int, date: String) = authAction.async { implicit request =>
    val startTime = System.currentTimeMillis()

    if (!isValidDate(date) || customerId <= 0) {
      val elapsed = System.currentTimeMillis() - startTime
      scala.concurrent.Future.successful(
        BadRequest(ApiResponse.errorResponse("Invalid customer ID or date format. Date must be YYYY-MM-DD"))
      )
    } else {
      eventsService.getEventsByDate(customerId, date).map {
        case Some(page) =>
          val elapsed = System.currentTimeMillis() - startTime
          logger.info(s"Retrieved ${page.totalCount} events for customer $customerId on $date in ${elapsed}ms")
          Ok(ApiResponse.toJson(ApiResponse.success(
            s"Retrieved ${page.totalCount} behavioral events for customer $customerId on $date",
            page
          )))

        case None =>
          val elapsed = System.currentTimeMillis() - startTime
          logger.warn(s"No events found for customer $customerId on $date in ${elapsed}ms")
          NotFound(ApiResponse.notFoundResponse(s"No behavioral events found for customer $customerId on $date"))

      }.recover { case ex =>
        val elapsed = System.currentTimeMillis() - startTime
        logger.error(s"Error retrieving events for customer $customerId on $date in ${elapsed}ms: ${ex.getMessage}")
        InternalServerError(ApiResponse.errorResponse(s"Failed to retrieve events: ${ex.getMessage}"))
      }
    }
  }

  private def isValidDate(date: String): Boolean = date.matches("\\d{4}-\\d{2}-\\d{2}")

  def getRecentEvents(customerId: Int, limit: Option[Int]) = authAction.async { implicit request =>
    val actualLimit = limit.getOrElse(50)
    val startTime = System.currentTimeMillis()

    if (customerId <= 0) {
      val elapsed = System.currentTimeMillis() - startTime
      scala.concurrent.Future.successful(
        BadRequest(ApiResponse.errorResponse("Customer ID must be a positive integer"))
      )
    } else {
      eventsService.getRecentEvents(customerId, actualLimit).map {
        case Some(page) =>
          val elapsed = System.currentTimeMillis() - startTime
          logger.info(s"Retrieved ${page.totalCount} recent events for customer $customerId in ${elapsed}ms")
          Ok(ApiResponse.toJson(ApiResponse.success(
            s"Retrieved last ${page.totalCount} behavioral events for customer $customerId (limit: $actualLimit)",
            page
          )))

        case None =>
          val elapsed = System.currentTimeMillis() - startTime
          logger.warn(s"No recent events found for customer $customerId in ${elapsed}ms")
          NotFound(ApiResponse.notFoundResponse(s"No recent behavioral events found for customer $customerId"))

      }.recover { case ex =>
        val elapsed = System.currentTimeMillis() - startTime
        logger.error(s"Error retrieving recent events for customer $customerId in ${elapsed}ms: ${ex.getMessage}")
        InternalServerError(ApiResponse.errorResponse(s"Failed to retrieve recent events: ${ex.getMessage}"))
      }
    }
  }

  def getAllEventsByDate(date: String) = authAction.async { implicit request =>
    val startTime = System.currentTimeMillis()

    if (!isValidDate(date)) {
      val elapsed = System.currentTimeMillis() - startTime
      scala.concurrent.Future.successful(
        BadRequest(ApiResponse.errorResponse("Invalid date format. Date must be YYYY-MM-DD"))
      )
    } else {
      eventsService.getAllEventsByDate(date).map {
        case Some(events) =>
          val elapsed = System.currentTimeMillis() - startTime
          logger.info(s"Retrieved ${events.length} total events for date $date in ${elapsed}ms")
          Ok(ApiResponse.toJson(ApiResponse.success(
            s"Retrieved ${events.length} behavioral events across all customers on $date",
            events
          )))

        case None =>
          val elapsed = System.currentTimeMillis() - startTime
          logger.warn(s"No events found for date $date in ${elapsed}ms")
          NotFound(ApiResponse.notFoundResponse(s"No behavioral events found for date $date"))

      }.recover { case ex =>
        val elapsed = System.currentTimeMillis() - startTime
        logger.error(s"Error retrieving events for date $date in ${elapsed}ms: ${ex.getMessage}")
        InternalServerError(ApiResponse.errorResponse(s"Failed to retrieve events: ${ex.getMessage}"))
      }
    }
  }
}

