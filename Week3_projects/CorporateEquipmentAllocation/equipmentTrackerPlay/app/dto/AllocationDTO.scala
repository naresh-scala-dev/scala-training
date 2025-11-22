package dto

import models.Allocation
import play.api.libs.json._

import java.sql.Timestamp

case class AllocationDTO(
                          equipment: EquipmentDTO,
                          employee: EmployeeDTO,
                          allocatedAt: Timestamp,
                          expectedReturn: Timestamp,
                          returnedAt: Option[Timestamp],
                          equipmentCondition: Option[String]
                        )

object AllocationDTO {
  implicit val timestampFormat: Format[Timestamp] = new Format[Timestamp] {
    def writes(ts: Timestamp): JsValue = JsString(ts.toString)

    def reads(json: JsValue): JsResult[Timestamp] = json match {
      case JsString(s) =>
        try JsSuccess(Timestamp.valueOf(s)) catch {
          case _: Exception => JsError("Invalid timestamp")
        }
      case _ => JsError("String expected")
    }
  }

  implicit val format: OFormat[AllocationDTO] = Json.format[AllocationDTO]

  def fromAllocation(
                      alloc: Allocation,
                      equipment: EquipmentDTO,
                      employee: EmployeeDTO
                    ): AllocationDTO = AllocationDTO(
    equipment = equipment,
    employee = employee,
    allocatedAt = alloc.allocatedAt,
    expectedReturn = alloc.expectedReturn,
    returnedAt = alloc.returnedAt,
    equipmentCondition = alloc.equipmentCondition
  )
}
