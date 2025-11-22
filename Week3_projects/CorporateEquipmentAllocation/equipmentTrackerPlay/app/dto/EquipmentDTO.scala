package dto


import models.Equipment
import play.api.libs.json.{Json, OFormat}

case class EquipmentDTO(
                         id: Long,
                         name: String,
                         `type`: String,
                         status: String
                       )

object EquipmentDTO {
  implicit val format: OFormat[EquipmentDTO] = Json.format[EquipmentDTO]

  def fromEquipment(e: Equipment): EquipmentDTO =
    EquipmentDTO(e.id, e.name, e.`type`, e.status)
}