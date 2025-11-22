package dto

import java.sql.Timestamp
import play.api.libs.json._
import utils.JsonTimestamp._
case class TaskUpdateDTO(
                          eventId: Option[Long] = None,
                          teamId: Option[Long] = None,
                          description: Option[String] = None,
                          status: Option[String] = None,
                          startTime: Option[Timestamp] = None,
                          endTime: Option[Timestamp] = None,
                          specialRequest: Option[String] = None,
                          createdAt: Option[Timestamp] = None,
                          updatedAt: Option[Timestamp] = None
                        )

object TaskUpdateDTO {
  implicit val format: OFormat[TaskUpdateDTO] = Json.format[TaskUpdateDTO]
}
