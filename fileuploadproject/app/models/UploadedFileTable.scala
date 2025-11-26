package models

import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag

class UploadedFileTable(tag: Tag) extends Table[UploadedFile](tag, "uploaded_files") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def filename = column[String]("filename")
  def path = column[String]("path")

  def * = (id, filename, path) <> ((UploadedFile.apply _).tupled, UploadedFile.unapply)
}
