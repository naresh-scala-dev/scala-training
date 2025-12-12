package repository

import models.{EventUser, EventUserTable}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EventUserRepository @Inject()(
                                     protected val dbConfigProvider: DatabaseConfigProvider
                                   )(implicit ec: ExecutionContext) {

  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import dbConfig.profile.api._

  private val users = TableQuery[EventUserTable]

  /** Validate user during login */
  def findByUsernamePassword(username: String, password: String): Future[Option[EventUser]] = {
    db.run(users.filter(u => u.username === username && u.password === password).result.headOption)
  }

  /** Get user by username (used in JWT auth) */
  def findByUsername(username: String): Future[Option[EventUser]] = {
    db.run(users.filter(_.username === username).result.headOption)
  }
}
