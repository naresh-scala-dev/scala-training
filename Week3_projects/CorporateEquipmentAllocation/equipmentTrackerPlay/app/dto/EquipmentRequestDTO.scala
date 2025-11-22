package dto

import play.api.libs.json.Json

case class EquipmentRequestDTO(
                                name: String,
                                `type`: String,
                                status: String
                              )

object EquipmentRequestDTO {
  implicit val format = Json.format[EquipmentRequestDTO]
}