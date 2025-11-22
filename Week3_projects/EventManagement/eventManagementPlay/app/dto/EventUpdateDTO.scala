package dto

import java.sql.Timestamp
import play.api.libs.json._
import utils.JsonTimestamp._
case class EventUpdateDTO(
                           name: Option[String] = None,
                           eventType: Option[String] = None,
                           date: Option[Timestamp] = None,
                           guestCount: Option[Int] = None
                         )

object EventUpdateDTO {
  implicit val format: OFormat[EventUpdateDTO] = Json.format[EventUpdateDTO]
}
