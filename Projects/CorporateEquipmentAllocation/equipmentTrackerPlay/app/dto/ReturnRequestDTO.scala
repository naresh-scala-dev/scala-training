package dto

case class ReturnRequestDTO(equipmentId: Long, userId: Long, condition: String)

object ReturnRequestDTO {
  implicit val format = play.api.libs.json.Json.format[ReturnRequestDTO]
}
