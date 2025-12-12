package models

import play.api.libs.json.{Json, OFormat}

case class LoginRequest(
                         username: String,
                         password: String
                       )

object LoginRequest {
  implicit val format: OFormat[LoginRequest] = Json.format[LoginRequest]
}

case class LoginResponse(
                          token: String,
                          expiresIn: Long,
                          username: String

                        )

object LoginResponse {
  implicit val format: OFormat[LoginResponse] = Json.format[LoginResponse]
}