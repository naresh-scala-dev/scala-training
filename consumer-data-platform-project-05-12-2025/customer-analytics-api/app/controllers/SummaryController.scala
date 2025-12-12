package controllers

import play.api.mvc._
import play.api.libs.json.Json
import com.typesafe.scalalogging.LazyLogging
import services.SummaryService
import models.{AggregatedDailySummary, ApiResponse, DailySummary}
import security.AuthAction

import javax.inject._
import scala.concurrent.ExecutionContext

@Singleton
class SummaryController @Inject()(
                                   cc: ControllerComponents,
                                   summaryService: SummaryService,
                                   authAction: AuthAction
                                 )(implicit ec: ExecutionContext) extends AbstractController(cc) with LazyLogging {

  implicit val dailySummaryWrites = DailySummary.dailySummaryFormat
  implicit val aggregatedSummaryWrites = AggregatedDailySummary.aggregatedDailySummaryFormat

  def getSummary(date: String, customerId: Int) = authAction.async { implicit request =>
    val startTime = System.currentTimeMillis()

    if (!isValidDate(date) || customerId <= 0) {
      val elapsed = System.currentTimeMillis() - startTime
      scala.concurrent.Future.successful(
        BadRequest(ApiResponse.errorResponse("Invalid customer ID or date format. Date must be YYYY-MM-DD"))
      )
    } else {
      summaryService.getDailySummary(date, customerId).map {
        case Some(summary) =>
          val elapsed = System.currentTimeMillis() - startTime
          logger.info(s"Retrieved daily summary for customer $customerId on $date in ${elapsed}ms")
          Ok(ApiResponse.toJson(ApiResponse.success(
            s"Daily transaction summary for customer $customerId on $date retrieved successfully",
            summary
          )))

        case None =>
          val elapsed = System.currentTimeMillis() - startTime
          logger.warn(s"No summary found for customer $customerId on $date in ${elapsed}ms")
          NotFound(ApiResponse.notFoundResponse(s"No transaction summary found for customer $customerId on $date"))

      }.recover { case ex =>
        val elapsed = System.currentTimeMillis() - startTime
        logger.error(s"Error retrieving summary for customer $customerId on $date in ${elapsed}ms: ${ex.getMessage}")
        InternalServerError(ApiResponse.errorResponse(s"Failed to retrieve daily summary: ${ex.getMessage}"))
      }
    }
  }

  private def isValidDate(date: String): Boolean = date.matches("\\d{4}-\\d{2}-\\d{2}")

  def getAllSummaries(date: String) = authAction.async { implicit request =>
    val startTime = System.currentTimeMillis()

    if (!isValidDate(date)) {
      val elapsed = System.currentTimeMillis() - startTime
      scala.concurrent.Future.successful(
        BadRequest(ApiResponse.errorResponse("Invalid date format. Date must be YYYY-MM-DD"))
      )
    } else {
      summaryService.getAllDailySummaries(date).map {
        case Some(agg) =>
          val elapsed = System.currentTimeMillis() - startTime
          logger.info(s"Retrieved aggregated summary for $date with ${agg.totalCustomers} customers in ${elapsed}ms")
          Ok(ApiResponse.toJson(ApiResponse.success(
            s"Aggregated transaction summary for $date retrieved successfully (${agg.totalCustomers} customers, total amount: ${agg.totalAmount})",
            agg
          )))

        case None =>
          val elapsed = System.currentTimeMillis() - startTime
          logger.warn(s"No summaries found for date $date in ${elapsed}ms")
          NotFound(ApiResponse.notFoundResponse(s"No transaction summaries found for date $date"))

      }.recover { case ex =>
        val elapsed = System.currentTimeMillis() - startTime
        logger.error(s"Error retrieving summaries for date $date in ${elapsed}ms: ${ex.getMessage}")
        InternalServerError(ApiResponse.errorResponse(s"Failed to retrieve aggregated summaries: ${ex.getMessage}"))
      }
    }
  }
}