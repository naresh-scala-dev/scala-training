package dto

import play.api.libs.json.{JsValue, Json, OFormat}

case class ApiResponse(
                        status: String,
                        message: String,
                        data: Option[JsValue] = None
                      )

object ApiResponse {
  implicit val format: OFormat[ApiResponse] = Json.format[ApiResponse]
}
