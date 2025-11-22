package controllers

import dto.{ApiResponse, EventUserCreateDTO, EventUserUpdateDTO}
import models.{EventUser, EventUserTable}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json._
import play.api.mvc._
import security.{AuthAction, Roles}
import slick.jdbc.JdbcProfile
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import java.sql.Timestamp
import utils.JsonTimestamp._

@Singleton
class EventUserController @Inject()(
                                     val controllerComponents: ControllerComponents,
                                     val dbConfigProvider: DatabaseConfigProvider,
                                     authAction: AuthAction
                                   )(implicit ec: ExecutionContext)
  extends BaseController
    with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  private val users = TableQuery[EventUserTable]


  implicit val eventUserFormat: OFormat[EventUser] = Json.format[EventUser]
  implicit val createDTOFormat: OFormat[EventUserCreateDTO] = Json.format[EventUserCreateDTO]
  implicit val updateDTOFormat: OFormat[EventUserUpdateDTO] = Json.format[EventUserUpdateDTO]

  /** Create new EventUser */
  def createUser: Action[JsValue] =
    authAction.withRoles(Set(Roles.EventManager)).async(parse.json) { request =>
      request.body.validate[EventUserCreateDTO].fold(
        _ => Future.successful(Ok(Json.toJson(ApiResponse("fail", "Invalid user data")))),
        dto => {
          db.run(users.filter(u => u.username === dto.username || u.email === dto.email).result.headOption).flatMap {
            case Some(_) =>
              Future.successful(Ok(Json.toJson(ApiResponse("fail", "Username or email already exists"))))
            case None =>
              val now = new Timestamp(System.currentTimeMillis())
              val newUser = EventUser(
                0L,
                dto.username,
                dto.email,
                dto.password,
                dto.userRole,
                Some(now),
                Some(now)
              )
              val insertQuery = (users returning users.map(_.id) into ((u, id) => u.copy(id = id))) += newUser
              db.run(insertQuery).map { created =>
                Ok(Json.toJson(ApiResponse("success", "User created successfully", Some(Json.toJson(created)))))
              }.recover {
                case ex => Ok(Json.toJson(ApiResponse("fail", s"Error creating user: ${ex.getMessage}")))
              }
          }
        }
      )
    }

  /** Get all EventUsers */
  def getAllUsers: Action[AnyContent] =
    authAction.withRoles(Set(Roles.EventManager)).async { _ =>
      db.run(users.result).map { list =>
        Ok(Json.toJson(ApiResponse("success", "Users fetched successfully", Some(Json.toJson(list)))))
      }.recover {
        case ex => Ok(Json.toJson(ApiResponse("fail", s"Error fetching users: ${ex.getMessage}")))
      }
    }

  /** Get single EventUser */
  def getUser(id: Long): Action[AnyContent] =
    authAction.async { request =>
      db.run(users.filter(_.id === id).result.headOption).map {
        case Some(user) =>
          if (request.role == Roles.EventManager || request.username == user.username) {
            Ok(Json.toJson(ApiResponse("success", "User fetched successfully", Some(Json.toJson(user)))))
          } else {
            Ok(Json.toJson(ApiResponse("fail", "Not authorized")))
          }
        case None =>
          Ok(Json.toJson(ApiResponse("fail", "User not found")))
      }.recover {
        case ex => Ok(Json.toJson(ApiResponse("fail", s"Error fetching user: ${ex.getMessage}")))
      }
    }

  /** Update EventUser */
  def updateUser(id: Long): Action[JsValue] =
    authAction.withRoles(Set(Roles.EventManager)).async(parse.json) { request =>
      request.body.validate[EventUserUpdateDTO].fold(
        _ => Future.successful(Ok(Json.toJson(ApiResponse("fail", "Invalid user data")))),
        dto => {
          val action = users.filter(_.id === id).result.headOption.flatMap {
            case Some(existing) =>
              val updated = existing.copy(
                username = dto.username.getOrElse(existing.username),
                password = dto.password.getOrElse(existing.password),
                email = dto.email.getOrElse(existing.email),
                userRole = dto.userRole.getOrElse(existing.userRole),
                createdAt = dto.createdAt.orElse(existing.createdAt),
                updatedAt = Some(new Timestamp(System.currentTimeMillis()))
              )
              users.filter(_.id === id).update(updated).map(_ => Some(updated))
            case None => DBIO.successful(None)
          }
          db.run(action.transactionally).map {
            case Some(_) => Ok(Json.toJson(ApiResponse("success", "User updated successfully")))
            case None => Ok(Json.toJson(ApiResponse("fail", "User not found")))
          }.recover {
            case ex => Ok(Json.toJson(ApiResponse("fail", s"Error updating user: ${ex.getMessage}")))
          }
        }
      )
    }

  /** Delete EventUser */
  def deleteUser(id: Long): Action[AnyContent] =
    authAction.withRoles(Set(Roles.EventManager)).async { _ =>
      db.run(users.filter(_.id === id).delete).map {
        case 0 => Ok(Json.toJson(ApiResponse("fail", "User not found")))
        case _ => Ok(Json.toJson(ApiResponse("success", "User deleted successfully")))
      }.recover {
        case ex => Ok(Json.toJson(ApiResponse("fail", s"Error deleting user: ${ex.getMessage}")))
      }
    }
}
