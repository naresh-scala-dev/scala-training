package controllers

import dto.{ApiResponse, EventCreateDTO, EventUpdateDTO}
import models.{Event, EventTable}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json._
import play.api.mvc._
import security.{AuthAction, Roles}
import slick.jdbc.JdbcProfile
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import utils.JsonTimestamp._

@Singleton
class EventController @Inject()(
                                 val controllerComponents: ControllerComponents,
                                 val dbConfigProvider: DatabaseConfigProvider,
                                 authAction: AuthAction
                               )(implicit ec: ExecutionContext)
  extends BaseController
    with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  private val events = TableQuery[EventTable]
  implicit val eventFormat: OFormat[Event] = Json.format[Event]

  /** Get all events (everyone can view) */
  def getAllEvents: Action[AnyContent] = authAction.async { _ =>
    db.run(events.result).map { list =>
      Ok(Json.toJson(ApiResponse("success", "Events fetched successfully", Some(Json.toJson(list)))))
    }
  }

  /** Get single event */
  def getEvent(id: Long): Action[AnyContent] = authAction.async { _ =>
    db.run(events.filter(_.id === id).result.headOption).map {
      case Some(event) => Ok(Json.toJson(ApiResponse("success", "Event fetched successfully", Some(Json.toJson(event)))))
      case None => Ok(Json.toJson(ApiResponse("fail", "Event not found")))
    }
  }

  /** Create new event (EventManager only) */
  def createEvent: Action[JsValue] = authAction.withRoles(Set(Roles.EventManager)).async(parse.json) { request =>
    request.body.validate[EventCreateDTO].fold(
      _ => Future.successful(Ok(Json.toJson(ApiResponse("fail", "Invalid event data")))),
      dto => {
        val duplicateCheck = events.filter(e => e.name === dto.name && e.date === dto.date).result.headOption
        db.run(duplicateCheck).flatMap {
          case Some(_) =>
            Future.successful(Ok(Json.toJson(ApiResponse("fail", "Event with same name and date already exists"))))
          case None =>
            val now = new java.sql.Timestamp(System.currentTimeMillis())
            val newEvent = Event(0L, dto.name, dto.eventType, dto.date, dto.guestCount, request.id, Some(now), Some(now))
            val insertQuery = (events returning events.map(_.id) into ((e, id) => e.copy(id = id))) += newEvent
            db.run(insertQuery).map { created =>
              Ok(Json.toJson(ApiResponse("success", "Event created successfully", Some(Json.toJson(created)))))
            }.recover {
              case ex => Ok(Json.toJson(ApiResponse("fail", s"Error creating event: ${ex.getMessage}")))
            }
        }
      }
    )
  }


  /** Update event (EventManager only) */
  def updateEvent(id: Long): Action[JsValue] = authAction.withRoles(Set(Roles.EventManager)).async(parse.json) { request =>
    request.body.validate[EventUpdateDTO].fold(
      _ => Future.successful(Ok(Json.toJson(ApiResponse("fail", "Invalid event data")))),
      dto => {
        val action = events.filter(_.id === id).result.headOption.flatMap {
          case Some(existing) =>
            val updated = existing.copy(
              name = dto.name.getOrElse(existing.name),
              eventType = dto.eventType.getOrElse(existing.eventType),
              date = dto.date.getOrElse(existing.date),
              guestCount = dto.guestCount.getOrElse(existing.guestCount)
            )
            events.filter(_.id === id).update(updated).map(_ => Some(updated))
          case None => DBIO.successful(None)
        }
        db.run(action.transactionally).map {
          case Some(_) => Ok(Json.toJson(ApiResponse("success", "Event updated successfully")))
          case None => Ok(Json.toJson(ApiResponse("fail", "Event not found")))
        }.recover {
          case ex => Ok(Json.toJson(ApiResponse("fail", s"Error updating event: ${ex.getMessage}")))
        }
      }
    )
  }

  /** Delete event (EventManager only) */
  def deleteEvent(id: Long): Action[AnyContent] = authAction.withRoles(Set(Roles.EventManager)).async { _ =>
    db.run(events.filter(_.id === id).delete).map {
      case 0 => Ok(Json.toJson(ApiResponse("fail", "Event not found")))
      case _ => Ok(Json.toJson(ApiResponse("success", "Event deleted successfully")))
    }.recover {
      case ex => Ok(Json.toJson(ApiResponse("fail", s"Error deleting event: ${ex.getMessage}")))
    }
  }
}
