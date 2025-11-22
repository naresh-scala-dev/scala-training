package models

import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag

import java.sql.Timestamp

class TeamTable(tag: Tag) extends Table[Team](tag, "teams") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def createdAt = column[Option[Timestamp]]("created_at")
  def updatedAt = column[Option[Timestamp]]("updated_at")

  def * = (id, name, createdAt, updatedAt) <> ((Team.apply _).tupled, Team.unapply)
}
