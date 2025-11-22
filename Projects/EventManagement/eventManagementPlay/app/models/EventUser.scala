package models

import java.sql.Timestamp

case class EventUser(
                      id: Long,
                      username: String,
                      email: String,
                      password: String,
                      userRole: String,
                      createdAt: Option[Timestamp],
                      updatedAt: Option[Timestamp]
                    )
