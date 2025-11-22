package dto

import java.sql.Timestamp
import play.api.libs.json._
import utils.JsonTimestamp._

case class TaskCreateDTO(
                          eventId: Long,
                          teamId: Long,
                          description: String,
                          status: String,
                          startTime: Timestamp,
                          endTime: Timestamp,
                          specialRequest: Option[String] = None,
                          createdAt: Option[Timestamp] = None,
                          updatedAt: Option[Timestamp] = None
                        )

object TaskCreateDTO {
  implicit val format: OFormat[TaskCreateDTO] = Json.format[TaskCreateDTO]
}
