package models

import play.api.libs.json._

case class EventsPage(
                       customerId: Int,
                       eventDate: String,
                       totalCount: Int,
                       events: List[CustomerEvent]
                     )

object EventsPage {
  implicit val eventsPageFormat: OFormat[EventsPage] = Json.format[EventsPage]
}
