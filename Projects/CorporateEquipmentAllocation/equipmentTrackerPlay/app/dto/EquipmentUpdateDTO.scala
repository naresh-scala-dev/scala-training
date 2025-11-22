package dto

import play.api.libs.json.Json

case class EquipmentUpdateDTO(
                               id: Long,
                               name: Option[String],
                               `type`: Option[String],
                               status: Option[String]
                             )

object EquipmentUpdateDTO {
  implicit val format = Json.format[EquipmentUpdateDTO]
}