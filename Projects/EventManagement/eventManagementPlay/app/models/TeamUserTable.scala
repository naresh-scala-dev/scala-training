package models
import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag
class TeamUserTable(tag: Tag) extends Table[TeamUser](tag, "team_users") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def teamId = column[Long]("team_id")
  def userId = column[Long]("user_id")

  def * = (id, teamId, userId) <> ((TeamUser.apply _).tupled, TeamUser.unapply)
}