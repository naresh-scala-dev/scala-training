package dto

import java.sql.Timestamp
import play.api.libs.json._
import utils.JsonTimestamp._
case class EventCreateDTO(
                           name: String,
                           eventType: String,
                           date: Timestamp,
                           guestCount: Int
                         )

object EventCreateDTO {
  implicit val format: OFormat[EventCreateDTO] = Json.format[EventCreateDTO]
}
