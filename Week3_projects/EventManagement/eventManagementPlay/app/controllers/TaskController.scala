package controllers

import dto.{ApiResponse, TaskCreateDTO, TaskUpdateDTO}
import models.{EventTable, Task, TaskTable}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json._
import play.api.mvc._
import security.{AuthAction, Roles}
import slick.jdbc.JdbcProfile
import services.KafkaProducerService
import repositories.TeamUserRepository

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import java.sql.Timestamp

@Singleton
class TaskController @Inject()(
                                val controllerComponents: ControllerComponents,
                                val dbConfigProvider: DatabaseConfigProvider,
                                authAction: AuthAction,
                                teamUserRepo: TeamUserRepository,
                                kafkaProducer: KafkaProducerService
                              )(implicit ec: ExecutionContext)
  extends BaseController
    with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  private val tasks = TableQuery[TaskTable]
  private val events = TableQuery[EventTable] // assuming you have EventTable with name, date fields

  implicit val timestampFormat: Format[Timestamp] = new Format[Timestamp] {
    def writes(ts: Timestamp): JsValue = JsString(ts.toString)
    def reads(json: JsValue): JsResult[Timestamp] = json match {
      case JsString(s) =>
        try JsSuccess(Timestamp.valueOf(s)) catch { case _: Exception => JsError("Invalid timestamp") }
      case _ => JsError("String expected")
    }
  }

  implicit val taskFormat: OFormat[Task] = Json.format[Task]

  /** Create Task + send notification */
  /** Create Task + send notification */
  def createTask: Action[JsValue] = authAction.withRoles(Set(Roles.EventManager)).async(parse.json) { request =>
    request.body.validate[TaskCreateDTO].fold(
      errors => Future.successful(Ok(Json.toJson(ApiResponse("fail", "Invalid task data")))),
      dto => {
        val now = new Timestamp(System.currentTimeMillis())

        val existsQuery = tasks.filter(t =>
          t.eventId === dto.eventId &&
            t.teamId === dto.teamId &&
            t.description === dto.description
        ).result.headOption

        db.run(existsQuery).flatMap {
          case Some(_) =>
            Future.successful(Ok(Json.toJson(ApiResponse("fail", "Task already exists"))))

          case None =>
            val newTask = Task(
              id = 0,
              eventId = dto.eventId,
              teamId = dto.teamId,
              description = dto.description,
              status = dto.status,
              startTime = dto.startTime,
              endTime = dto.endTime,
              specialRequest = dto.specialRequest,
              reminderSent = false,
              eventDayAlertSent = false,
              createdAt = Some(now),
              updatedAt = Some(now)
            )

            val insertQuery = (tasks returning tasks.map(_.id) into ((t, id) => t.copy(id = id))) += newTask

            db.run(insertQuery).flatMap { createdTask =>
              val eventQuery = events.filter(_.id === createdTask.eventId).result.headOption

              db.run(eventQuery).flatMap {
                case Some(event) =>
                  teamUserRepo.getEmailsByTeamId(createdTask.teamId).map { emails =>
                    emails.foreach { email =>
                      val payload = Json.obj(
                        "eventType" -> "TASK_ASSIGNMENT",
                        "eventName" -> event.name,
                        "eventDate" -> event.date.toString,
                        "description" -> createdTask.description,
                        "userEmail" -> email,
                        "scheduledTime" -> createdTask.startTime.toString,
                        "specialRequest" -> Json.toJson(createdTask.specialRequest.getOrElse(""))
                      )
                      println("TASK_ASSIGNMENT payload: " + payload.toString())
                      kafkaProducer.sendEvent("TASK_ASSIGNMENT", payload.toString())
                    }
                  }.map(_ => Ok(Json.toJson(ApiResponse("success", "Task created successfully", Some(Json.toJson(createdTask))))))

                case None =>
                  Future.successful(Ok(Json.toJson(ApiResponse("fail", "Event not found"))))
              }
            }
        }
      }
    )
  }

  /** Update Task + send notification on status change */
  def updateTask(id: Long): Action[JsValue] = authAction.withRoles(Set(Roles.EventManager)).async(parse.json) { request =>
    println(s"[updateTask] Received request to update task id=$id at ${new Timestamp(System.currentTimeMillis())}")

    request.body.validate[TaskUpdateDTO].fold(
      errors => {
        println(s"[updateTask] Invalid task data: $errors")
        Future.successful(Ok(Json.toJson(ApiResponse("fail", "Invalid task data"))))
      },
      dto => {
        println(s"[updateTask] Valid DTO received: $dto")

        val action = tasks.filter(_.id === id).result.headOption.flatMap {
          case Some(existing) =>
            println(s"[updateTask] Found existing task: $existing")

            val updated = existing.copy(
              eventId = dto.eventId.getOrElse(existing.eventId),
              teamId = dto.teamId.getOrElse(existing.teamId),
              description = dto.description.getOrElse(existing.description),
              status = dto.status.getOrElse(existing.status),
              startTime = dto.startTime.getOrElse(existing.startTime),
              endTime = dto.endTime.getOrElse(existing.endTime),
              specialRequest = dto.specialRequest.orElse(existing.specialRequest),
              updatedAt = Some(new Timestamp(System.currentTimeMillis()))
            )
            println(s"[updateTask] Prepared updated task: $updated")

            tasks.filter(t =>
              t.id =!= id &&
                t.eventId === updated.eventId &&
                t.teamId === updated.teamId &&
                t.description === updated.description
            ).result.headOption.flatMap {
              case Some(_) =>
                println(s"[updateTask] Duplicate task exists, aborting update.")
                DBIO.successful(None)
              case None =>
                println(s"[updateTask] No duplicate found, proceeding with update.")
                tasks.filter(_.id === id).update(updated).map(_ => Some((existing, updated)))
            }

          case None =>
            println(s"[updateTask] Task not found for id=$id")
            DBIO.successful(None)
        }

        db.run(action.transactionally).flatMap {
          case Some((existing, updated)) =>
            println(s"[updateTask] Update successful. Existing status=${existing.status}, Updated status=${updated.status}")

            if (existing.status != updated.status) {
              println(s"[updateTask] Status changed, sending STATUS_UPDATE emails.")

              val eventQuery = events.filter(_.id === updated.eventId).result.headOption
              db.run(eventQuery).flatMap {
                case Some(event) =>
                  println(s"[updateTask] Found event: $event")

                  teamUserRepo.getEmailsByTeamId(updated.teamId).map { emails =>
                    println(s"[updateTask] Emails to notify: $emails")
                    emails.foreach { email =>
                      val payload = Json.obj(
                        "eventType" -> "STATUS_UPDATE",
                        "eventName" -> event.name,
                        "eventDate" -> event.date.toString,
                        "description" -> updated.description,
                        "userEmail" -> email,
                        "oldStatus" -> existing.status,
                        "newStatus" -> updated.status,
                        "updatedAt" -> Json.toJson(updated.updatedAt.get)
                      )
                      println(s"[updateTask] Sending STATUS_UPDATE to $email: $payload")
                      kafkaProducer.sendEvent("STATUS_UPDATE", payload.toString())
                    }
                  }.map(_ => Ok(Json.toJson(ApiResponse("success", "Task updated successfully"))))

                case None =>
                  println(s"[updateTask] Event not found for eventId=${updated.eventId}")
                  Future.successful(Ok(Json.toJson(ApiResponse("fail", "Event not found"))))
              }
            } else {
              println(s"[updateTask] Status did not change, no email sent.")
              Future.successful(Ok(Json.toJson(ApiResponse("success", "Task updated successfully"))))
            }

          case None =>
            println(s"[updateTask] Task not updated (either not found or duplicate exists)")
            Future.successful(Ok(Json.toJson(ApiResponse("fail", "Task not found or duplicate exists"))))
        }
      }
    )
  }

  /** Delete Task */
  def deleteTask(id: Long): Action[AnyContent] = authAction.withRoles(Set(Roles.EventManager)).async { _ =>
    db.run(tasks.filter(_.id === id).delete).map {
      case 0 => Ok(Json.toJson(ApiResponse("fail", "Task not found")))
      case _ => Ok(Json.toJson(ApiResponse("success", "Task deleted successfully")))
    }
  }

  /** Get all tasks */
  def getAllTasks: Action[AnyContent] = authAction.withRoles(Set(Roles.EventManager, Roles.TeamMember)).async { _ =>
    db.run(tasks.result).map(list =>
      Ok(Json.toJson(ApiResponse("success", "Tasks fetched successfully", Some(Json.toJson(list)))))
    )
  }

  /** Get single task */
  def getTask(id: Long): Action[AnyContent] = authAction.withRoles(Set(Roles.EventManager, Roles.TeamMember)).async { _ =>
    db.run(tasks.filter(_.id === id).result.headOption).map {
      case Some(task) => Ok(Json.toJson(ApiResponse("success", "Task fetched", Some(Json.toJson(task)))))
      case None => Ok(Json.toJson(ApiResponse("fail", "Task not found")))
    }
  }
}
