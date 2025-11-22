package filters

import dto.ApiResponse
import org.apache.pekko.stream.Materializer
import play.api.libs.json.Json
import play.api.mvc._
import security.JWTUtils

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthFilter @Inject()(implicit val mat: Materializer, ec: ExecutionContext) extends Filter {

  override def apply(next: RequestHeader => Future[Result])(request: RequestHeader): Future[Result] = {


    if (request.path == "/api/login") {
      return next(request)
    }

    if (request.path.startsWith("/api/")) {
      request.headers.get("Authorization") match {
        case Some(h) if h.startsWith("Bearer ") =>
          val token = h.substring(7)
          JWTUtils.verifyToken(token) match {
            case Right((username, role)) =>
              next(request)
            case Left(error) =>
              Future.successful(Results.Unauthorized(Json.toJson(ApiResponse("fail", error))))
          }

        case _ =>
          Future.successful(Results.Unauthorized(Json.toJson(ApiResponse("fail", "Missing Authorization header"))))
      }
    } else {
      next(request)
    }
  }
}
