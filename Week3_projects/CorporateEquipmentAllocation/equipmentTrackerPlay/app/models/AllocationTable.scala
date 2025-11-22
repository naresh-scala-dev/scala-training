package models

import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag

import java.sql.Timestamp

class AllocationTable(tag: Tag) extends Table[Allocation](tag, "allocations") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def equipmentId = column[Long]("equipment_id")

  def userId = column[Long]("user_id")

  def allocatedAt = column[Timestamp]("allocated_at")

  def expectedReturn = column[Timestamp]("expected_return")

  def returnedAt = column[Option[Timestamp]]("returned_at")

  def equipmentCondition = column[Option[String]]("equipment_condition")

  def reminderSent = column[Boolean]("reminder_sent", O.Default(false))

  def * =
    (id, equipmentId, userId, allocatedAt, expectedReturn, returnedAt, equipmentCondition, reminderSent)
      .<>(Allocation.tupled, Allocation.unapply)
}
