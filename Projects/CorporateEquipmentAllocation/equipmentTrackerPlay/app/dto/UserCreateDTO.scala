package dto

import play.api.libs.json.{Json, OFormat}

case class UserCreateDTO(
                          username: String,
                          password: String,
                          role: String,
                          name: String,
                          department: String,
                          email: String
                        )

object UserCreateDTO {
  implicit val format: OFormat[UserCreateDTO] = Json.format[UserCreateDTO]
}
