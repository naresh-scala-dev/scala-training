package models

import java.sql.Timestamp

case class Event(
                  id: Long,
                  name: String,
                  eventType: String,
                  date: Timestamp,
                  guestCount: Int,
                  createdBy: Long, // references EventUser.id
                  createdAt: Option[Timestamp],
                  updatedAt: Option[Timestamp]
                )
