package dto

import models.User
import play.api.libs.json.{Json, OFormat}

case class EmployeeDTO(
                        id: Long,
                        name: String,
                        department: String
                      )

object EmployeeDTO {
  implicit val format: OFormat[EmployeeDTO] = Json.format[EmployeeDTO]

  // map from User DB model
  def fromUser(user: User): EmployeeDTO = {
    EmployeeDTO(
      id = user.id,
      name = user.name, // if name is Option[String] in User, use user.name.getOrElse("")
      department = user.department // same here
    )
  }
}
