package models


import play.api.mvc._


case class EventUserRequest[A](
                                username: String,
                                role: String,
                                request: Request[A]
                              ) extends WrappedRequest[A](request)
