package service

import play.api.libs.json.{Json, OFormat}

case class TaskEvent(
                      eventType: String,
                      taskId: Option[Int] = None,
                      description: Option[String] = None,
                      eventId: Option[Int] = None,
                      eventName: Option[String] = None,
                      eventDate: Option[String] = None,
                      userEmail: Option[String] = None,
                      scheduledTime: Option[String] = None,
                      oldStatus: Option[String] = None,
                      newStatus: Option[String] = None,
                      specialRequest: Option[String] = None
                    )

object TaskEvent {
  implicit val format: OFormat[TaskEvent] = Json.format[TaskEvent]
}
