package models

import play.api.libs.json._

case class ApiResponse[T](
                           status: String,
                           message: String,
                           data: Option[T] = None,
                           error: Option[String] = None
                         )

object ApiResponse {

  // Helper method to create success response
  def success[T](data: T, message: String, responseTime: Option[Long] = None): ApiResponse[T] = {
    ApiResponse(
      status = "SUCCESS",
      message = message,
      data = Some(data),
      error = None
    )
  }

  // Overloaded success for backward compatibility
  def success[T](message: String, data: T): ApiResponse[T] = {
    ApiResponse(
      status = "SUCCESS",
      message = message,
      data = Some(data),
      error = None
    )
  }

  // Helper method to create bad request response
  def badRequest[T](message: String, responseTime: Option[Long] = None): ApiResponse[T] = {
    ApiResponse(
      status = "BAD_REQUEST",
      message = message,
      data = None,
      error = Some("INVALID_REQUEST")
    )
  }

  // Helper method to create error response with error code
  def error[T](message: String, errorCode: String, responseTime: Option[Long] = None): ApiResponse[T] = {
    ApiResponse(
      status = "ERROR",
      message = message,
      data = None,
      error = Some(errorCode)
    )
  }

  // Helper method to create not found response
  def notFound[T](message: String, responseTime: Option[Long] = None): ApiResponse[T] = {
    ApiResponse(
      status = "NOT_FOUND",
      message = message,
      data = None,
      error = Some("RESOURCE_NOT_FOUND")
    )
  }

  // Helper method to create not found response (backward compatibility)
  def notFoundResponse(message: String): JsObject = {
    Json.obj(
      "status" -> "NOT_FOUND",
      "message" -> message,
      "data" -> JsNull,
      "error" -> "RESOURCE_NOT_FOUND"
    )
  }

  // Helper method to create error response (backward compatibility)
  def errorResponse(message: String): JsObject = {
    Json.obj(
      "status" -> "ERROR",
      "message" -> message,
      "data" -> JsNull,
      "error" -> "INTERNAL_ERROR"
    )
  }

  // Custom method to convert ApiResponse to JSON
  def toJson[T](response: ApiResponse[T])(implicit tWrites: Writes[T]): JsObject = {
    val baseJson = Json.obj(
      "status" -> response.status,
      "message" -> response.message
    )

    val withData = response.data match {
      case Some(d) => baseJson + ("data" -> tWrites.writes(d))
      case None => baseJson + ("data" -> JsNull)
    }

    response.error match {
      case Some(err) => withData + ("error" -> Json.toJson(err))
      case None => withData
    }
  }

  // Implicit Writes for automatic JSON conversion
  implicit def apiResponseWrites[T](implicit tWrites: Writes[T]): Writes[ApiResponse[T]] = new Writes[ApiResponse[T]] {
    def writes(response: ApiResponse[T]): JsValue = {
      toJson(response)(tWrites)
    }
  }
}