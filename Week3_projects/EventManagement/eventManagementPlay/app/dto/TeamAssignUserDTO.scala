package dto

case class TeamAssignUserDTO(
                              teamId: Long,
                              userIds: Seq[Long]
                            )
