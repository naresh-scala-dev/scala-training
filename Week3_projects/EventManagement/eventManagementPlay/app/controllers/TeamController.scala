package controllers

import dto.{ApiResponse, TeamAssignUserDTO, TeamCreateDTO, TeamUpdateDTO}
import models.{Team, TeamTable, TeamUser, TeamUserTable}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json._
import play.api.mvc._
import security.{AuthAction, Roles}
import slick.jdbc.JdbcProfile

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import utils.JsonTimestamp._

@Singleton
class TeamController @Inject()(
                                val controllerComponents: ControllerComponents,
                                val dbConfigProvider: DatabaseConfigProvider,
                                authAction: AuthAction
                              )(implicit ec: ExecutionContext)
  extends BaseController
    with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  private val teams = TableQuery[TeamTable]
  private val teamUsers = TableQuery[TeamUserTable]

  implicit val teamFormat: OFormat[Team] = Json.format[Team]
  implicit val teamUserFormat: OFormat[TeamUser] = Json.format[TeamUser]

  /** Create Team with validation for duplicates */
  def createTeam: Action[JsValue] = authAction.withRoles(Set(Roles.EventManager)).async(parse.json) { request =>
    request.body.validate[TeamCreateDTO].fold(
      _ => Future.successful(Ok(Json.toJson(ApiResponse("fail", "Invalid input data")))),
      dto => {
        val checkDuplicate = teams.filter(_.name === dto.name).result.headOption
        db.run(checkDuplicate).flatMap {
          case Some(_) =>
            Future.successful(Ok(Json.toJson(ApiResponse("fail", s"Team '${dto.name}' already exists"))))
          case None =>
            val now = new java.sql.Timestamp(System.currentTimeMillis())
            val newTeam = Team(0, dto.name, Some(now), Some(now))
            val insertQuery = (teams returning teams.map(_.id) into ((t, id) => t.copy(id = id))) += newTeam
            db.run(insertQuery).map { created =>
              Ok(Json.toJson(ApiResponse("success", "Team created successfully", Some(Json.toJson(created)))))
            }.recover {
              case ex => Ok(Json.toJson(ApiResponse("fail", s"Error creating team: ${ex.getMessage}")))
            }
        }
      }
    )
  }


  /** Get all Teams */
  def getAllTeams: Action[AnyContent] = authAction.withRoles(Set(Roles.EventManager)).async { _ =>
    db.run(teams.result).map { list =>
      Ok(Json.toJson(ApiResponse("success", "Teams fetched successfully", Some(Json.toJson(list)))))
    }.recover {
      case ex => Ok(Json.toJson(ApiResponse("fail", s"Error fetching teams: ${ex.getMessage}")))
    }
  }

  /** Get single Team */
  def getTeam(id: Long): Action[AnyContent] = authAction.withRoles(Set(Roles.EventManager)).async { _ =>
    db.run(teams.filter(_.id === id).result.headOption).map {
      case Some(team) => Ok(Json.toJson(ApiResponse("success", "Team fetched", Some(Json.toJson(team)))))
      case None => Ok(Json.toJson(ApiResponse("fail", "Team not found")))
    }.recover {
      case ex => Ok(Json.toJson(ApiResponse("fail", s"Error fetching team: ${ex.getMessage}")))
    }
  }

  /** Update Team with duplicate check */
  def updateTeam(id: Long): Action[JsValue] = authAction.withRoles(Set(Roles.EventManager)).async(parse.json) { request =>
    request.body.validate[TeamUpdateDTO].fold(
      _ => Future.successful(Ok(Json.toJson(ApiResponse("fail", "Invalid input data")))),
      dto => {
        val action = teams.filter(_.id === id).result.headOption.flatMap {
          case Some(existing) =>
            val updatedName = dto.name.getOrElse(existing.name)
            // Check for duplicate name in other teams
            teams.filter(t => t.id =!= id && t.name === updatedName).result.headOption.flatMap {
              case Some(_) => DBIO.successful(None)
              case None =>
                val updated = existing.copy(
                  name = updatedName,
                  createdAt = dto.createdAt.orElse(existing.createdAt),
                  updatedAt = dto.updatedAt.orElse(existing.updatedAt)
                )
                teams.filter(_.id === id).update(updated).map(_ => Some(updated))
            }
          case None => DBIO.successful(None)
        }

        db.run(action.transactionally).map {
          case Some(_) => Ok(Json.toJson(ApiResponse("success", "Team updated successfully")))
          case None => Ok(Json.toJson(ApiResponse("fail", "Team not found or duplicate name exists")))
        }.recover {
          case ex => Ok(Json.toJson(ApiResponse("fail", s"Error updating team: ${ex.getMessage}")))
        }
      }
    )
  }

  /** Delete Team */
  def deleteTeam(id: Long): Action[AnyContent] = authAction.withRoles(Set(Roles.EventManager)).async { _ =>
    db.run(teams.filter(_.id === id).delete).map {
      case 0 => Ok(Json.toJson(ApiResponse("fail", "Team not found")))
      case _ => Ok(Json.toJson(ApiResponse("success", "Team deleted successfully")))
    }.recover {
      case ex => Ok(Json.toJson(ApiResponse("fail", s"Error deleting team: ${ex.getMessage}")))
    }
  }

  /** Assign Users to Team with duplicate prevention and meaningful messages */
  def assignUsersToTeam: Action[JsValue] = authAction.withRoles(Set(Roles.EventManager)).async(parse.json) { request =>
    implicit val assignFormat = Json.format[dto.TeamAssignUserDTO]

    request.body.validate[dto.TeamAssignUserDTO].fold(
      _ => Future.successful(Ok(Json.toJson(ApiResponse("fail", "Invalid input data")))),
      dto => {
        val actions = dto.userIds.map { uid =>
          teamUsers.filter(tu => tu.teamId === dto.teamId && tu.userId === uid).result.headOption.flatMap {
            case Some(_) => DBIO.successful(0) // already assigned
            case None => teamUsers += TeamUser(0L, dto.teamId, uid)
          }
        }


        db.run(DBIO.sequence(actions).transactionally).map { results =>
          val assignedCount = results.count(_ != 0)            // number of newly inserted users
          val alreadyAssignedCount = results.size - assignedCount // number of users already in the team

          val message =
            if (assignedCount == 0) "All users are already assigned to this team"
            else if (alreadyAssignedCount > 0)
              s"$assignedCount user(s) assigned, $alreadyAssignedCount user(s) were already in the team"
            else
              s"$assignedCount user(s) successfully assigned to team ${dto.teamId}"

          Ok(Json.toJson(ApiResponse("success", message)))
        }.recover {
          case ex => Ok(Json.toJson(ApiResponse("fail", s"Error assigning users: ${ex.getMessage}")))
        }
      }
    )
  }


  /** Remove User from Team */
  def removeUserFromTeam: Action[JsValue] = authAction.withRoles(Set(Roles.EventManager)).async(parse.json) { request =>
    implicit val assignFormat = Json.format[TeamAssignUserDTO]

    request.body.validate[TeamAssignUserDTO].fold(
      _ => Future.successful(Ok(Json.toJson(ApiResponse("fail", "Invalid input data")))),
      dto => {
        val action = teamUsers.filter(tu => tu.teamId === dto.teamId && tu.userId.inSet(dto.userIds)).delete
        db.run(action).map { deleted =>
          Ok(Json.toJson(ApiResponse("success", s"$deleted user(s) removed from team ${dto.teamId}")))
        }.recover {
          case ex => Ok(Json.toJson(ApiResponse("fail", s"Error removing users: ${ex.getMessage}")))
        }
      }
    )
  }
}
