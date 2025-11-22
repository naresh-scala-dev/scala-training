package controllers

import dto.{ApiResponse, LoginDTO}
import play.api.libs.json._
import play.api.mvc._
import repositories.UserRepository
import security.JWTUtils

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthController @Inject()(
                                cc: ControllerComponents,
                                userRepo: UserRepository
                              )(implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  def login(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[LoginDTO].fold(
      _ => Future.successful(BadRequest(Json.toJson(ApiResponse("fail", "Invalid JSON")))),
      loginData => {
        userRepo.findUserByUsernamePassword(loginData.username, loginData.password).map {
          case Some(user) =>
            val (token, expiresIn) = JWTUtils.createToken(user.username, user.role)
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
