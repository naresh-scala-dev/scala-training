package models

import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag

import java.sql.Timestamp

class TaskTable(tag: Tag) extends Table[Task](tag, "tasks") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def eventId = column[Long]("event_id")
  def teamId = column[Long]("team_id")
  def description = column[String]("description")
  def status = column[String]("status")
  def startTime = column[Timestamp]("start_time")
  def endTime = column[Timestamp]("end_time")
  def specialRequest = column[Option[String]]("special_request")
  def createdAt = column[Option[Timestamp]]("created_at")
  def updatedAt = column[Option[Timestamp]]("updated_at")

  def * = (id, eventId, teamId, description, status, startTime, endTime, specialRequest, createdAt, updatedAt) <> ((Task.apply _).tupled, Task.unapply)

  def eventFK = foreignKey("fk_event", eventId, TableQuery[EventTable])(_.id, onDelete = ForeignKeyAction.Cascade)
  def teamFK = foreignKey("fk_team", teamId, TableQuery[TeamTable])(_.id)
}
