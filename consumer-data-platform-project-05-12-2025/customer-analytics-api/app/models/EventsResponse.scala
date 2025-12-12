package models

import play.api.libs.json.{Json, OFormat}

case class EventsResponse(
                           customerId: Int,
                           totalCount: Int,
                           limit: Option[Int],
                           events: List[CustomerEvent]
                         )

object EventsResponse {
  implicit val eventsResponseFormat: OFormat[EventsResponse] = Json.format[EventsResponse]
}