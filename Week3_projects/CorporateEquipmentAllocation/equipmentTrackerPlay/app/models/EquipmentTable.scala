package models

import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag

class EquipmentTable(tag: Tag) extends Table[Equipment](tag, "equipment") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def name = column[String]("name")

  def `type` = column[String]("type")

  def status = column[String]("status")

  def * = (id, name, `type`, status) <> ((Equipment.apply _).tupled, Equipment.unapply)
}