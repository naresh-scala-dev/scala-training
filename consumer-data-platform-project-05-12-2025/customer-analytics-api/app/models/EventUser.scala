package models

import play.api.libs.json.{Json, OFormat}
import java.sql.Timestamp
import config.JsonTimestamp._

case class EventUser(
                      id: Long = 0L,
                      username: String,
                      email: String,
                      password: String,
                      userRole: String,
                      createdAt: Option[Timestamp] = None,
                      updatedAt: Option[Timestamp] = None
                    )

object EventUser {
  implicit val format: OFormat[EventUser] = Json.format[EventUser]
}
