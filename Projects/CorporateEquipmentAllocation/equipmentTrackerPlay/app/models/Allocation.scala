package models

import java.sql.Timestamp

case class Allocation(
                       id: Long,
                       equipmentId: Long,
                       userId: Long,
                       allocatedAt: Timestamp,
                       expectedReturn: Timestamp,
                       returnedAt: Option[Timestamp],
                       equipmentCondition: Option[String],
                       reminderSent: Boolean
                     )
