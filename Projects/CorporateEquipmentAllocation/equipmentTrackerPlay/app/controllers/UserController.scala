package controllers

import dto.{ApiResponse, UserCreateDTO, UserUpdateDTO}
import models.{User, UserRequest, UserTable}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json._
import play.api.mvc._
import security.{AuthAction, Roles}
import slick.jdbc.JdbcProfile

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserController @Inject()(
                                val controllerComponents: ControllerComponents,
                                val dbConfigProvider: DatabaseConfigProvider,
                                authAction: AuthAction
                              )(implicit ec: ExecutionContext)
  extends BaseController
    with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  private val users = TableQuery[UserTable]
  implicit val userFormat: OFormat[User] = Json.format[User]

  /** Only admin can create users */
  def createUser: Action[JsValue] = authAction.withRoles(Set(Roles.Admin)).async(parse.json) { request =>
    request.body.validate[UserCreateDTO].fold(
      _ => Future.successful(Ok(Json.toJson(ApiResponse("fail", "Invalid user data")))),
      dto => {
        db.run(users.filter(_.username === dto.username).result.headOption).flatMap {
          case Some(_) =>
            Future.successful(Ok(Json.toJson(ApiResponse("fail", s"Username '${dto.username}' already exists"))))
          case None =>
            val newUser = User(0, dto.username, dto.password, dto.role, dto.name, dto.department, dto.email)
            val insert = (users returning users.map(_.id)) += newUser
            db.run(insert).map { id =>
              Ok(Json.toJson(ApiResponse("success", "User created successfully", Some(Json.toJson(newUser.copy(id = id))))))
            }
        }
      }
    )
  }

  /** Only admin can view all users */
  def getAllUsers: Action[AnyContent] = authAction.withRoles(Set(Roles.Admin)).async { _ =>
    db.run(users.result).map(list =>
      Ok(Json.toJson(ApiResponse("success", "Users fetched successfully", Some(Json.toJson(list)))))
    )
  }

  /** Admin can update users */
  def updateUser(id: Long): Action[JsValue] = authAction.withRoles(Set(Roles.Admin)).async(parse.json) { request =>
    request.body.validate[UserUpdateDTO].fold(
      _ => Future.successful(Ok(Json.toJson(ApiResponse("fail", "Invalid user data")))),
      dto => {
        val action = users.filter(_.id === id).result.headOption.flatMap {
          case Some(existing) =>
            val updated = existing.copy(
              username = dto.username.getOrElse(existing.username),
              password = dto.password.getOrElse(existing.password),
              role = dto.role.getOrElse(existing.role),
              name = dto.name.getOrElse(existing.name),
              department = dto.department.getOrElse(existing.department)
            )
            users.filter(_.id === id).update(updated).map(_ => Some(updated))
          case None => DBIO.successful(None)
        }
        db.run(action.transactionally).map {
          case Some(_) => Ok(Json.toJson(ApiResponse("success", "User updated successfully")))
          case None => Ok(Json.toJson(ApiResponse("fail", "User not found")))
        }
      }
    )
  }

  /** Only admin can delete users */
  def deleteUser(id: Long): Action[AnyContent] = authAction.withRoles(Set(Roles.Admin)).async { _ =>
    db.run(users.filter(_.id === id).delete).map {
      case 0 => Ok(Json.toJson(ApiResponse("fail", "User not found")))
      case _ => Ok(Json.toJson(ApiResponse("success", "User deleted successfully")))
    }
  }

  /** Get a single user: Admin can fetch any, others only themselves */
  def getUser(id: Long): Action[AnyContent] = authAction.async { request: UserRequest[AnyContent] =>
    db.run(users.filter(_.id === id).result.headOption).map {
      case Some(user) =>
        if (request.role == Roles.Admin || request.id == id) {
          Ok(Json.toJson(ApiResponse("success", "User fetched successfully", Some(Json.toJson(user)))))
        } else {
          Ok(Json.toJson(ApiResponse("fail", "Not authorized")))
        }
      case None =>
        Ok(Json.toJson(ApiResponse("fail", "User not found")))
    }
  }
}
