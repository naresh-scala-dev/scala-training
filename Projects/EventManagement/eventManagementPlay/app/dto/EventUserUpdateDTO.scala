package dto

import java.sql.Timestamp
import utils.JsonTimestamp._


case class EventUserUpdateDTO(
                               username: Option[String] = None,
                               email: Option[String] = None,
                               password: Option[String] = None,
                               userRole: Option[String] = None,
                               createdAt: Option[Timestamp] = None,
                               updatedAt: Option[Timestamp] = None
                             )

object EventUserUpdateDTO {
  import play.api.libs.json._
  implicit val format: OFormat[EventUserUpdateDTO] = Json.format[EventUserUpdateDTO]
}