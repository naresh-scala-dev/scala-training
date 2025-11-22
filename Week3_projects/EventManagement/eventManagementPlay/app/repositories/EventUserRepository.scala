package repositories

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

  /** Find user by username & password (for login) */
  def findByUsernamePassword(username: String, password: String): Future[Option[EventUser]] = {
    val query = users.filter(u => u.username === username && u.password === password).result.headOption
    db.run(query)
  }

  /** Find user by username only (for token verification etc.) */
  def findByUsername(username: String): Future[Option[EventUser]] = {
    val query = users.filter(_.username === username).result.headOption
    db.run(query)
  }

  /** Create a new user */
  def createUser(user: EventUser): Future[EventUser] = {
    val insertQuery = (users returning users.map(_.id)
      into ((userRow, id) => userRow.copy(id = id))) += user.copy(id = 0L)

    db.run(insertQuery)
  }


  /** Update an existing user */
  def update(user: EventUser): Future[Int] = {
    val query = users.filter(_.id === user.id)
      .map(u => (u.username, u.password, u.userRole, u.createdAt, u.updatedAt))
      .update((user.username, user.password, user.userRole, user.createdAt, user.updatedAt))
    db.run(query)
  }

  /** Delete a user */
  def delete(id: Long): Future[Int] = {
    db.run(users.filter(_.id === id).delete)
  }

  /** Get all users */
  def getAll: Future[Seq[EventUser]] = {
    db.run(users.result)
  }

  /** Get a single user by ID */
  def getById(id: Long): Future[Option[EventUser]] = {
    db.run(users.filter(_.id === id).result.headOption)
  }
  def getEventManagerEmail(): Future[Option[String]] = {
    val query = users.filter(_.userRole === "EVENT_MANAGER").map(_.email).result.headOption
    db.run(query)
  }


}
