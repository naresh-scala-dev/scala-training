package controllers

import play.api.mvc._
import play.api.libs.json.Json
import com.typesafe.scalalogging.LazyLogging
import services.CustomerService
import models.{ApiResponse, CustomerProfile}
import javax.inject._
import scala.concurrent.ExecutionContext
import security.AuthAction

@Singleton
class CustomerController @Inject()(
                                    cc: ControllerComponents,
                                    customerService: CustomerService,
                                    authAction: AuthAction
                                  )(implicit ec: ExecutionContext) extends AbstractController(cc) with LazyLogging {

  implicit val profileWrites = CustomerProfile.customerProfileFormat

  def getCustomer(id: Int) = authAction.async { implicit request =>
    val startTime = System.currentTimeMillis()

    if (id <= 0) {
      val elapsed = System.currentTimeMillis() - startTime
      scala.concurrent.Future.successful(
        BadRequest(ApiResponse.errorResponse("Customer ID must be a positive integer"))
      )
    } else {
      customerService.getCustomerProfile(id).map {
        case Some(profile) =>
          val elapsed = System.currentTimeMillis() - startTime
          logger.info(s"Customer profile retrieved for ID $id in ${elapsed}ms")
          Ok(ApiResponse.toJson(ApiResponse.success(
            s"Customer profile for ${profile.name} retrieved successfully",
            profile
          )))

        case None =>
          val elapsed = System.currentTimeMillis() - startTime
          logger.warn(s"Customer ID $id not found in ${elapsed}ms")
          NotFound(ApiResponse.notFoundResponse(s"Customer with ID $id does not exist"))

      }.recover { case ex =>
        val elapsed = System.currentTimeMillis() - startTime
        logger.error(s"Error retrieving customer $id in ${elapsed}ms: ${ex.getMessage}")
        InternalServerError(ApiResponse.errorResponse(s"Failed to retrieve customer profile: ${ex.getMessage}"))
      }
    }
  }
}