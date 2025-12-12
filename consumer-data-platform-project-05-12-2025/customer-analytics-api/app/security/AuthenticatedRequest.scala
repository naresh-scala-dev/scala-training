package security

import play.api.mvc.{Request, WrappedRequest}

case class AuthenticatedRequest[A](username: String, request: Request[A])
  extends WrappedRequest[A](request)
