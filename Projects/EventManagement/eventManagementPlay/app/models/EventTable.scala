package models

import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag
import java.sql.Timestamp

class EventTable(tag: Tag) extends Table[Event](tag, "events") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def eventType = column[String]("type")
  def date = column[Timestamp]("date")
  def guestCount = column[Int]("guest_count")
  def createdBy = column[Long]("created_by")
  def createdAt = column[Option[Timestamp]]("created_at")
  def updatedAt = column[Option[Timestamp]]("updated_at")

  def * = (id, name, eventType, date, guestCount, createdBy, createdAt, updatedAt) <> ((Event.apply _).tupled, Event.unapply)
}
