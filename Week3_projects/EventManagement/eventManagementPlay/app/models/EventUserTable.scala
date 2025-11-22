package models

import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag
import java.sql.Timestamp

class EventUserTable(tag: Tag) extends Table[EventUser](tag, "event_users") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def username = column[String]("username")
  def email = column[String]("email")
  def password = column[String]("password")
  def userRole = column[String]("user_role")
  def createdAt = column[Option[Timestamp]]("created_at")
  def updatedAt = column[Option[Timestamp]]("updated_at")

  def * = (id, username, email, password, userRole, createdAt, updatedAt) <> ((EventUser.apply _).tupled, EventUser.unapply)
}

