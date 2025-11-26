package controllers

import javax.inject._
import play.api.mvc._
import java.io.File
import java.nio.file.Paths
import scala.concurrent.{ExecutionContext, Future}
import repositories.UploadedFileRepo
import models.UploadedFile
import play.api.libs.json.Json

@Singleton
class FileController @Inject()(cc: ControllerComponents, uploadedFileRepo: UploadedFileRepo)
                              (implicit ec: ExecutionContext) extends AbstractController(cc) {

  val uploadDir = new File("uploads")
  if (!uploadDir.exists()) uploadDir.mkdirs()

  // POST /upload
  def upload = Action(parse.multipartFormData).async { request =>
    request.body.file("file").map { filePart =>
      val filename = Paths.get(filePart.filename).getFileName.toString
      val uniqueFilename = java.util.UUID.randomUUID().toString + "_" + filename
      val filePath = new File(uploadDir, uniqueFilename)
      filePart.ref.moveTo(filePath, replace = true)

      val uploadedFile = UploadedFile(filename = filename, path = filePath.getAbsolutePath)
      uploadedFileRepo.insert(uploadedFile).map { saved =>
        Ok(Json.obj(
          "status" -> "success",
          "file" -> Json.toJson(saved)
        ))
      }
    }.getOrElse {
      Future.successful(BadRequest(Json.obj("status" -> "fail", "message" -> "No file uploaded")))
    }
  }

  // GET /files
  def listFiles = Action.async {
    uploadedFileRepo.listAll().map { files =>
      Ok(Json.toJson(files))
    }
  }
}
