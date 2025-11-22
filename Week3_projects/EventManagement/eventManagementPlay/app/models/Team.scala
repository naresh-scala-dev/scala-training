package models

import java.sql.Timestamp

case class Team(
                 id: Long,
                 name: String,
                 createdAt: Option[Timestamp],
                 updatedAt: Option[Timestamp]
               )
