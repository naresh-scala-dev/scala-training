package repositories

import javax.inject._
import models.{UploadedFile, UploadedFileTable}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UploadedFileRepo @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {

  val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig._
  import profile.api._

  private val uploadedFiles = TableQuery[UploadedFileTable]

  def insert(file: UploadedFile): Future[UploadedFile] = {
    val insertAction = (uploadedFiles returning uploadedFiles.map(_.id)
      into ((file, id) => file.copy(id = id))) += file
    db.run(insertAction)
  }

  def listAll(): Future[Seq[UploadedFile]] = db.run(uploadedFiles.result)
}
