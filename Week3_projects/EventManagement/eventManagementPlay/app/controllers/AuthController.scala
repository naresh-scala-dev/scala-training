package controllers

import dto.{ApiResponse, LoginDTO}
import models.EventUser
import play.api.libs.json._
import play.api.mvc._
import security.JWTUtils
import repositories.EventUserRepository

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthController @Inject()(
                                cc: ControllerComponents,
                                userRepo: EventUserRepository
                              )(implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  implicit val loginFormat: OFormat[LoginDTO] = Json.format[LoginDTO]

  /** Login endpoint */
  def login(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[LoginDTO].fold(
      errors => Future.successful(BadRequest(Json.toJson(ApiResponse("fail", "Invalid JSON")))),
      loginData => {
        userRepo.findByUsernamePassword(loginData.username, loginData.password).map {
          case Some(user: EventUser) =>
            val (token, expiresIn) = JWTUtils.createToken(user.username, user.userRole)
            Ok(Json.toJson(ApiResponse(
              status = "success",
              message = "Login successful",
              data = Some(Json.obj("token" -> token, "expires_in" -> expiresIn))
            )))
          case None =>
            Unauthorized(Json.toJson(ApiResponse("fail", "Invalid username or password")))
        }
      }
    )
  }
}
