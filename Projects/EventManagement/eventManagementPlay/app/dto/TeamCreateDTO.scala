package dto

import java.sql.Timestamp
import play.api.libs.json._
import utils.JsonTimestamp._
case class TeamCreateDTO(
                          name: String,
                          createdAt: Option[Timestamp] = None,
                          updatedAt: Option[Timestamp] = None
                        )

object TeamCreateDTO {
  implicit val format: OFormat[TeamCreateDTO] = Json.format[TeamCreateDTO]
}
