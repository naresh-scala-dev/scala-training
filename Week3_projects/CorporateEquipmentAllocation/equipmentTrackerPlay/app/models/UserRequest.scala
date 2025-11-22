package models

import play.api.mvc._

case class UserRequest[A](username: String, role: String, request: Request[A]) extends WrappedRequest[A](request)
