package controllers

import com.typesafe.scalalogging.LazyLogging
import models.{ApiResponse, LoginRequest, LoginResponse}
import play.api.libs.json.Json
import play.api.mvc._
import repository.{EventUserRepository, CassandraRepository, ParquetRepository}
import security.JWTUtils
import services.{CustomerService, SummaryService, EventsService}
import cache.CacheService

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthController @Inject()(
                                cc: ControllerComponents,
                                userRepo: EventUserRepository,
                                cassandraRepo: CassandraRepository,
                                parquetRepo: ParquetRepository,
                                cacheService: CacheService,
                                customerService: CustomerService,
                                summaryService: SummaryService,
                                eventsService: EventsService
                              )(implicit ec: ExecutionContext)
  extends AbstractController(cc) with LazyLogging {

  /** POST /auth/login */
  def login() = Action.async(parse.json) { implicit request =>
    request.body.validate[LoginRequest].fold(
      errors =>
        Future.successful(BadRequest(Json.toJson(
          ApiResponse.badRequest[String](s"Invalid JSON: $errors", None)
        ))),

      loginReq => {
        userRepo.findByUsernamePassword(loginReq.username, loginReq.password).map {
          case Some(user) =>
            val (token, expiresIn) = JWTUtils.createToken(user.username)
            val response = LoginResponse(token, expiresIn, user.username)

            logger.info(s"User logged in: ${user.username}")

            // Asynchronously initialize connections for first time
            Future {
              cassandraRepo.initSession() // make sure this method exists in CassandraRepository
              parquetRepo.initS3Connection() // make sure this method exists in ParquetRepository
            }

            Ok(Json.toJson(ApiResponse.success(response, "Login successful", None)))

          case None =>
            Unauthorized(Json.toJson(ApiResponse.error[String](
              "Invalid username or password", "INVALID_CREDENTIALS", None
            )))
        }
      }
    )
  }
}
