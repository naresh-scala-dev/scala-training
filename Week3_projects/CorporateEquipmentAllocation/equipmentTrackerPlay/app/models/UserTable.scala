package models

import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag

class UserTable(tag: Tag) extends Table[User](tag, "users") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def username = column[String]("username")

  def password = column[String]("password")

  def role = column[String]("role")

  def name = column[String]("name")

  def department = column[String]("department")

  def email = column[String]("email")

  def * = (id, username, password, role, name, department, email) <> ((User.apply _).tupled, User.unapply)
}
