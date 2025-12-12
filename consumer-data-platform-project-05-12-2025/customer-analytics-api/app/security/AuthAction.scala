package security

import models.ApiResponse
import play.api.libs.json.Json
import play.api.mvc._
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthAction @Inject()(val parser: BodyParsers.Default)(
  implicit val executionContext: ExecutionContext
) extends ActionBuilder[AuthenticatedRequest, AnyContent] {

  override def invokeBlock[A](
                               request: Request[A],
                               block: AuthenticatedRequest[A] => Future[Result]
                             ): Future[Result] = {

    request.headers.get("Authorization") match {
      case Some(header) if header.startsWith("Bearer ") =>
        val token = header.substring(7)

        JWTUtils.verifyToken(token) match {
          case Right(username) =>
            block(AuthenticatedRequest(username, request))

          case Left(error) =>
            Future.successful(
              Results.Unauthorized(
                Json.toJson(ApiResponse.error[String](
                  error,
                  "UNAUTHORIZED",
                  None
                ))
              )
            )
        }

      case _ =>
        Future.successful(
          Results.Unauthorized(
            Json.toJson(ApiResponse.error[String](
              "Missing or invalid Authorization header",
              "UNAUTHORIZED",
              None
            ))
          )
        )
    }
  }
}
