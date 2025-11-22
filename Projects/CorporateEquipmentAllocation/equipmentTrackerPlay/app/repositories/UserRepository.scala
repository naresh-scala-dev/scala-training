package repositories

import models.{User, UserTable}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserRepository @Inject()(
                                protected val dbConfigProvider: DatabaseConfigProvider
                              )(implicit ec: ExecutionContext) {

  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import dbConfig.profile.api._

  private val users = TableQuery[UserTable]

  /** Find user by username & password (for login) */
  def findUserByUsernamePassword(username: String, password: String): Future[Option[User]] = {
    val query = users.filter(u => u.username === username && u.password === password).result.headOption
    db.run(query)
  }

  /** Create a new user */
  def createUser(user: User): Future[User] = {
    val insert = users returning users.map(_.id) into ((u, id) => u.copy(id = id))
    db.run(insert += user.copy(id = 0))
  }

  /** Update an existing user */
  def updateUser(user: User): Future[Int] = {
    val query = users.filter(_.id === user.id)
      .map(u => (u.username, u.password, u.role, u.name, u.department))
      .update((user.username, user.password, user.role, user.name, user.department))
    db.run(query)
  }

  /** Delete a user */
  def deleteUser(id: Long): Future[Int] = {
    db.run(users.filter(_.id === id).delete)
  }

  /** Get all users */
  def getAllUsers: Future[Seq[User]] = {
    db.run(users.result)
  }

  /** Get a single user by ID */
  def getUser(id: Long): Future[Option[User]] = {
    db.run(users.filter(_.id === id).result.headOption)
  }

  /** Get email for a given user ID */
  def getEmailById(userId: Long): Future[Option[String]] = {
    db.run(users.filter(_.id === userId).map(_.email).result.headOption)
  }
}
