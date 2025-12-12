package models

import play.api.libs.json.{Json, OFormat}

case class UploadedFile(id: Long = 0, filename: String, path: String)

object UploadedFile {
  implicit val format: OFormat[UploadedFile] = Json.format[UploadedFile]
}
