package repositories

import models.{EventUserTable, TeamUser, TeamUserTable}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TeamUserRepository @Inject()(
                                    protected val dbConfigProvider: DatabaseConfigProvider
                                  )(implicit ec: ExecutionContext) {

  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig._
  import dbConfig.profile.api._

  private val teamUsers = TableQuery[TeamUserTable]
  private val eventUsers = TableQuery[EventUserTable]

  /** Assign user to team */
  def addUserToTeam(teamUser: TeamUser): Future[TeamUser] = {
    val insertQuery = (teamUsers returning teamUsers.map(_.id)
      into ((tu, id) => tu.copy(id = id))) += teamUser.copy(id = 0L)
    db.run(insertQuery)
  }

  /** Remove user from team */
  def removeUserFromTeam(teamUserId: Long): Future[Int] = {
    db.run(teamUsers.filter(_.id === teamUserId).delete)
  }

  /** Get all users for a team */
  def getUsersByTeamId(teamId: Long): Future[Seq[Long]] = {
    val query = teamUsers.filter(_.teamId === teamId).map(_.userId)
    db.run(query.result)
  }

  /** Get all emails for a team */
  def getEmailsByTeamId(teamId: Long): Future[Seq[String]] = {
    val query = for {
      tu <- teamUsers if tu.teamId === teamId
      u  <- eventUsers if u.id === tu.userId
    } yield u.email // Or u.email if you have an email column
    db.run(query.result)
  }
}
