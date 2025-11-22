package dto

import java.sql.Timestamp
import utils.JsonTimestamp._


case class EventUserCreateDTO(
                               username: String,
                               email: String,
                               password: String, // added email
                               userRole: String,
                               createdAt: Option[Timestamp] = None,
                               updatedAt: Option[Timestamp] = None
                             )

object EventUserCreateDTO {
  import play.api.libs.json._
  implicit val format: OFormat[EventUserCreateDTO] = Json.format[EventUserCreateDTO]
}