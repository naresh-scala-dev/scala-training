package security

import dto.ApiResponse
import models.EventUserRequest
import play.api.libs.json.Json
import play.api.mvc._

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}



@Singleton
class AuthAction @Inject()(val parser: BodyParsers.Default)(implicit val executionContext: ExecutionContext)
  extends ActionBuilder[EventUserRequest, AnyContent] {

  override def invokeBlock[A](request: Request[A], block: EventUserRequest[A] => Future[Result]): Future[Result] = {

    request.headers.get("Authorization") match {
      case Some(h) if h.startsWith("Bearer ") =>
        val token = h.substring(7)

        JWTUtils.verifyToken(token) match {
          case Right((username, role)) =>
            block(EventUserRequest(username, role, request))
          case Left(error) =>
            Future.successful(Results.Unauthorized(Json.toJson(ApiResponse("fail", error))))
        }

      case _ =>
        Future.successful(Results.Unauthorized(Json.toJson(ApiResponse("fail", "Missing Authorization header"))))
    }
  }

  def withRoles(roles: Set[String]): ActionBuilder[EventUserRequest, AnyContent] = new ActionBuilder[EventUserRequest, AnyContent] {
    override def parser: BodyParser[AnyContent] = AuthAction.this.parser

    override protected def executionContext: ExecutionContext = AuthAction.this.executionContext

    override def invokeBlock[A](request: Request[A], block: EventUserRequest[A] => Future[Result]): Future[Result] = {
      AuthAction.this.invokeBlock[A](request, { userReq: EventUserRequest[A] =>
        if (roles.contains(userReq.role)) block(userReq)
        else Future.successful(Results.Forbidden(Json.toJson(ApiResponse("fail", "Not authorized"))))
      })
    }
  }
}
