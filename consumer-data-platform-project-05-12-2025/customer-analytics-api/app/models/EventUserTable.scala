package models

import slick.jdbc.MySQLProfile.api._
import slick.lifted.ProvenShape

import java.sql.Timestamp

class EventUserTable(tag: Tag) extends Table[EventUser](tag, "event_users") {

  override def * : ProvenShape[EventUser] =
    (id, username, email, password, userRole, createdAt, updatedAt) <>
      ((EventUser.apply _).tupled, EventUser.unapply)

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def username = column[String]("username", O.Unique)

  def email = column[String]("email")

  def password = column[String]("password")

  def userRole = column[String]("user_role")

  def createdAt = column[Option[Timestamp]]("created_at")

  def updatedAt = column[Option[Timestamp]]("updated_at")
}
