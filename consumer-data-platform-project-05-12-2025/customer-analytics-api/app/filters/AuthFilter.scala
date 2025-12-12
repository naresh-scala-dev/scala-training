package filters

import models.ApiResponse
import org.apache.pekko.stream.Materializer
import play.api.libs.json.Json
import play.api.mvc._
import security.JWTUtils

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthFilter @Inject()(implicit val mat: Materializer, ec: ExecutionContext) extends Filter {

  // Public routes that do not require authentication
  private val publicPaths: Set[String] = Set(
    "/auth/login",
    "/health",
    "/api/health"
  )

  override def apply(next: RequestHeader => Future[Result])(request: RequestHeader): Future[Result] = {

    // Allow public routes
    if (publicPaths.contains(request.path) || request.path == "/") {
      return next(request)
    }

    // Protect all secured routes
    if (request.path.startsWith("/api/") ||
      request.path.startsWith("/events") ||
      request.path.startsWith("/summary")) {

      request.headers.get("Authorization") match {

        // Authorization: Bearer XXXX
        case Some(header) if header.startsWith("Bearer ") =>
          val token = header.drop("Bearer ".length)

          JWTUtils.verifyToken(token) match {
            case Right(_) =>
              next(request)

            case Left(errorMessage: String) =>
              Future.successful(
                Results.Unauthorized(
                  Json.toJson(
                    ApiResponse.error[String](
                      errorMessage,
                      "UNAUTHORIZED",
                      None
                    )
                  )
                )
              )
          }

        case _ =>
          Future.successful(
            Results.Unauthorized(
              Json.toJson(
                ApiResponse.error[String](
                  "Missing Authorization header",
                  "UNAUTHORIZED",
                  None
                )
              )
            )
          )
      }
    } else {
      // For non-secured paths
      next(request)
    }
  }
}