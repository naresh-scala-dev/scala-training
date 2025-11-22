package models

import java.sql.Timestamp

case class Notification(
                         id: Long,
                         taskId: Long,
                         notificationType: String,
                         sentAt: Option[Timestamp],
                         status: Option[String],
                         createdAt: Option[Timestamp],
                         updatedAt: Option[Timestamp]
                       )
