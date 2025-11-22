package dto

import play.api.libs.json._

case class LoginDTO(username: String, password: String)

object LoginDTO {
  implicit val format: OFormat[LoginDTO] = Json.format[LoginDTO]
}
