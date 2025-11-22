package controllers

import dto._
import models._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json._
import play.api.mvc._
import security.{AuthAction, Roles}
import services.KafkaProducerService
import slick.jdbc.JdbcProfile

import java.sql.Timestamp
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EquipmentController @Inject()(
                                     val controllerComponents: ControllerComponents,
                                     val dbConfigProvider: DatabaseConfigProvider,
                                     authAction: AuthAction,
                                     kafkaProducer: KafkaProducerService
                                   )(implicit ec: ExecutionContext)
  extends BaseController
    with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  private val equipments = TableQuery[EquipmentTable]
  private val allocations = TableQuery[AllocationTable]
  private val users = TableQuery[UserTable]


  implicit val timestampFormat: Format[Timestamp] = new Format[Timestamp] {
    def writes(ts: Timestamp): JsValue = JsString(ts.toString)

    def reads(json: JsValue): JsResult[Timestamp] = json match {
      case JsString(s) =>
        try JsSuccess(Timestamp.valueOf(s))
        catch {
          case _: Exception => JsError("Invalid timestamp")
        }
      case _ => JsError("String expected")
    }
  }

  implicit val equipmentDTOFormat: OFormat[EquipmentDTO] = Json.format[EquipmentDTO]
  implicit val employeeDTOFormat: OFormat[EmployeeDTO] = Json.format[EmployeeDTO]
  implicit val allocationDTOFormat: OFormat[AllocationDTO] = Json.format[AllocationDTO]


  def listEquipment = authAction.withRoles(Set(Roles.Admin, Roles.InventoryStaff, Roles.ReceptionStaff)).async { _ =>
    db.run(equipments.result).map { list =>
      val data = list.map(e => EquipmentDTO.fromEquipment(e))
      Ok(Json.toJson(ApiResponse("success", "Equipment list fetched", Some(Json.toJson(data)))))
    }
  }

  def createEquipment = authAction.withRoles(Set(Roles.Admin)).async(parse.json) { request =>
    request.body.validate[EquipmentRequestDTO].fold(
      _ => Future.successful(Ok(Json.toJson(ApiResponse("fail", "Invalid equipment data", Some(Json.obj()))))),
      data => {
        val existsCheck = equipments.filter(e => e.name === data.name && e.`type` === data.`type`).result.headOption
        db.run(existsCheck).flatMap {
          case Some(_) =>
            Future.successful(Ok(Json.toJson(ApiResponse("fail", "Equipment already exists", Some(Json.obj())))))
          case None =>
            val newEquipment = Equipment(0, data.name, data.`type`, data.status)
            val insert = (equipments returning equipments.map(_.id)) += newEquipment
            db.run(insert).map { id =>
              val eqDTO = EquipmentDTO.fromEquipment(newEquipment.copy(id = id))
              Ok(Json.toJson(ApiResponse("success", "Equipment created successfully", Some(Json.obj("equipment" -> Json.toJson(eqDTO))))))
            }
        }
      }
    )
  }

  def updateEquipment = authAction.withRoles(Set(Roles.Admin, Roles.InventoryStaff)).async(parse.json) { request =>
    request.body.validate[EquipmentUpdateDTO].fold(
      _ => Future.successful(Ok(Json.toJson(ApiResponse("fail", "Invalid update data", Some(Json.obj()))))),
      data => {
        val query = equipments.filter(_.id === data.id)
        val action = query.result.headOption.flatMap {
          case Some(existing) =>
            val updated = existing.copy(
              name = data.name.getOrElse(existing.name),
              `type` = data.`type`.getOrElse(existing.`type`),
              status = data.status.getOrElse(existing.status)
            )
            query.update(updated).map(_ => Some(updated))
          case None => DBIO.successful(None)
        }
        db.run(action.transactionally).map {
          case Some(eq) =>
            Ok(Json.toJson(ApiResponse("success", "Equipment updated successfully", Some(Json.obj("equipment" -> Json.toJson(EquipmentDTO.fromEquipment(eq)))))))
          case None => Ok(Json.toJson(ApiResponse("fail", "Equipment not found", Some(Json.obj()))))
        }
      }
    )
  }


  def allocateEquipment = authAction.withRoles(Set(Roles.Admin, Roles.ReceptionStaff)).async(parse.json) { request =>
    request.body.validate[AllocationRequestDTO].fold(
      _ => Future.successful(Ok(Json.toJson(ApiResponse("fail", "Invalid allocation request", Some(Json.obj()))))),
      data => {
        val checkAllocated = allocations.filter(a => a.equipmentId === data.equipmentId && a.returnedAt.isEmpty).result.headOption
        val employeeQuery = users.filter(_.id === data.userId).result.headOption
        val inventoryQuery = users.filter(_.role === "inventory").result.headOption
        val equipmentQuery = equipments.filter(_.id === data.equipmentId).result.headOption

        val action = for {
          allocated <- checkAllocated
          emp <- employeeQuery
          inv <- inventoryQuery
          eq <- equipmentQuery
          result <- (allocated, eq, emp, inv) match {
            case (Some(_), _, _, _) => DBIO.successful(Left("Equipment is already allocated"))
            case (_, None, _, _) => DBIO.successful(Left("Equipment not found"))
            case (_, _, None, _) => DBIO.successful(Left("Employee not found"))
            case (_, _, _, None) => DBIO.successful(Left("Inventory user not found"))
            case (None, Some(equipment), Some(employee), Some(inventory)) =>
              val allocation = Allocation(0, data.equipmentId, data.userId, new Timestamp(System.currentTimeMillis()), Timestamp.valueOf(data.expectedReturn), None, None, false)
              val insert = allocations += allocation
              val updateStatus = equipments.filter(_.id === data.equipmentId).map(_.status).update("allocated")
              insert.andThen(updateStatus).map(_ => Right((allocation.copy(id = 0), employee, inventory, equipment)))
          }
        } yield result

        db.run(action.transactionally).map {
          case Left(err) => Ok(Json.toJson(ApiResponse("fail", err, Some(Json.obj()))))
          case Right((alloc, employee, inventory, equipment)) =>
            kafkaProducer.sendEvent("allocated", data.equipmentId, employee.email, inventory.email)
            val allocationDTO = AllocationDTO.fromAllocation(alloc, EquipmentDTO.fromEquipment(equipment), EmployeeDTO.fromUser(employee))
            Ok(Json.toJson(ApiResponse("success", "Equipment allocated successfully", Some(Json.obj("allocation" -> Json.toJson(allocationDTO))))))
        }
      }
    )
  }

  def returnEquipment = authAction.withRoles(Set(Roles.Admin, Roles.ReceptionStaff)).async(parse.json) { request =>
    request.body.validate[ReturnRequestDTO].fold(
      _ => Future.successful(Ok(Json.toJson(ApiResponse("fail", "Invalid return request", Some(Json.obj()))))),
      data => {
        val allocationQuery = allocations.filter(a => a.equipmentId === data.equipmentId && a.userId === data.userId && a.returnedAt.isEmpty).result.headOption
        val employeeQuery = users.filter(_.id === data.userId).result.headOption
        val inventoryQuery = users.filter(_.role === "inventory").result.headOption
        val equipmentQuery = equipments.filter(_.id === data.equipmentId).result.headOption
        val maintenanceQuery = users.filter(_.role === "maintenance").result.headOption

        val action = for {
          alloc <- allocationQuery
          emp <- employeeQuery
          inv <- inventoryQuery
          eq <- equipmentQuery
          maint <- maintenanceQuery
          result <- (alloc, emp, inv, eq) match {
            case (None, _, _, _) => DBIO.successful(Left("No active allocation found for this employee"))
            case (_, None, _, _) => DBIO.successful(Left("Employee not found"))
            case (_, _, None, _) => DBIO.successful(Left("Inventory user not found"))
            case (_, _, _, None) => DBIO.successful(Left("Equipment not found"))
            case (Some(a), Some(e), Some(i), Some(eq)) =>
              val updatedAlloc = a.copy(
                returnedAt = Some(new Timestamp(System.currentTimeMillis())),
                equipmentCondition = Some(data.condition)
              )
              val updateAlloc = allocations.filter(_.id === a.id)
                .map(a => (a.returnedAt, a.equipmentCondition))
                .update((updatedAlloc.returnedAt, updatedAlloc.equipmentCondition))

              val updateStatus = equipments.filter(_.id === data.equipmentId)
                .map(_.status)
                .update(if (data.condition.toLowerCase == "damaged") "damaged" else "available")

              updateAlloc.andThen(updateStatus).map(_ => Right((updatedAlloc, e, i, eq, maint)))
          }
        } yield result

        db.run(action.transactionally).map {
          case Left(err) => Ok(Json.toJson(ApiResponse("fail", err, Some(Json.obj()))))
          case Right((alloc, employee, inventory, equipment, maintenance)) =>

            kafkaProducer.sendEvent("returned", data.equipmentId, employee.email, inventory.email)

            if (data.condition.toLowerCase == "damaged") {
              kafkaProducer.sendEvent("damaged", data.equipmentId, employee.email, maintenance.map(_.email).getOrElse("maintenance@company.com"))
            }

            val allocationDTO = AllocationDTO.fromAllocation(alloc, EquipmentDTO.fromEquipment(equipment), EmployeeDTO.fromUser(employee))
            Ok(Json.toJson(ApiResponse("success", "Equipment returned successfully", Some(Json.obj("allocation" -> Json.toJson(allocationDTO))))))
        }
      }
    )
  }

  def allocationStatus(userId: Long) = authAction.withRoles(Set(Roles.Admin, Roles.ReceptionStaff, Roles.InventoryStaff)).async { _ =>
    val query = allocations.filter(_.userId === userId)
      .join(equipments).on(_.equipmentId === _.id)
      .join(users).on(_._1.userId === _.id)
      .result

    db.run(query).map { list =>
      if (list.isEmpty) {
        Ok(Json.toJson(ApiResponse("fail", s"No equipment allocated for user with ID $userId", Some(Json.arr()))))
      } else {
        val data = list.map { case ((alloc, eq), emp) =>
          AllocationDTO.fromAllocation(alloc, EquipmentDTO.fromEquipment(eq), EmployeeDTO.fromUser(emp))
        }

        Ok(Json.toJson(ApiResponse("success", "Allocation status fetched", Some(Json.toJson(data)))))
      }
    }
  }

  def markDamagedEquipment(equipmentId: Long) = authAction.withRoles(Set(Roles.Admin, Roles.MaintenanceStaff)).async { _ =>
    val maintenanceQuery = users.filter(_.role === "maintenance").result.headOption
    val inventoryQuery = users.filter(_.role === "inventory").result.headOption
    val equipmentQuery = equipments.filter(_.id === equipmentId).result.headOption

    val action = for {
      mOpt <- maintenanceQuery
      iOpt <- inventoryQuery
      eqOpt <- equipmentQuery
      updated <- eqOpt match {
        case Some(_) => equipments.filter(_.id === equipmentId).map(_.status).update("damaged")
        case None => DBIO.successful(0)
      }
    } yield (mOpt, iOpt, eqOpt, updated)

    db.run(action.transactionally).map {
      case (Some(m), Some(i), Some(eq), updated) if updated > 0 =>
        kafkaProducer.sendEvent("damaged", equipmentId, m.email, i.email)
        Ok(Json.toJson(ApiResponse("success", "Equipment marked as damaged", Some(Json.obj("equipment" -> Json.toJson(EquipmentDTO.fromEquipment(eq))))))
        )
      case (_, _, None, _) => Ok(Json.toJson(ApiResponse("fail", "Equipment not found", Some(Json.obj()))))
      case _ => Ok(Json.toJson(ApiResponse("fail", "Maintenance or Inventory user missing", Some(Json.obj()))))
    }
  }
}
