package dto

case class AllocationRequestDTO(userId: Long, equipmentId: Long, expectedReturn: String) // changed from employeeId

object AllocationRequestDTO {
  implicit val format = play.api.libs.json.Json.format[AllocationRequestDTO]
}
