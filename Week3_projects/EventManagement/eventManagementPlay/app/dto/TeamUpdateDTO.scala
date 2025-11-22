package dto

import java.sql.Timestamp
import play.api.libs.json._
import utils.JsonTimestamp._
case class TeamUpdateDTO(
                          name: Option[String] = None,
                          createdAt: Option[Timestamp] = None,
                          updatedAt: Option[Timestamp] = None
                        )

object TeamUpdateDTO {
  implicit val format: OFormat[TeamUpdateDTO] = Json.format[TeamUpdateDTO]
}
