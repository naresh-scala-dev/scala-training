package models

import java.sql.Timestamp

case class Task(
                 id: Long,
                 eventId: Long,
                 teamId: Long,
                 description: String,
                 status: String,
                 startTime: Timestamp,
                 endTime: Timestamp,
                 specialRequest: Option[String],
                 reminderSent: Boolean = false,
                 eventDayAlertSent: Boolean = false,
                 createdAt: Option[Timestamp],
                 updatedAt: Option[Timestamp]
               )
