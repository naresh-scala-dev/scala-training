package dto

import play.api.libs.json.{Json, OFormat}

case class UserUpdateDTO(
                          username: Option[String],
                          password: Option[String],
                          role: Option[String],
                          name: Option[String],
                          department: Option[String],
                          email: Option[String]
                        )

object UserUpdateDTO {
  implicit val format: OFormat[UserUpdateDTO] = Json.format[UserUpdateDTO]
}
