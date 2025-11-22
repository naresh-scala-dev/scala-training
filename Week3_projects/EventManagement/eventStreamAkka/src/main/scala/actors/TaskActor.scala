package actors

import akka.actor.{Actor, ActorLogging, ActorRef}
import NotificationActor._
import service.TaskEvent

object TaskActor {
  def props(notificationActor: ActorRef): akka.actor.Props =
    akka.actor.Props(new TaskActor(notificationActor))

  case class HandleTaskEvent(event: TaskEvent)
}

class TaskActor(notificationActor: ActorRef) extends Actor with ActorLogging {
  import TaskActor._

  override def receive: Receive = {
    case HandleTaskEvent(event) =>
      val userEmail = event.userEmail.getOrElse("")
      if (userEmail.nonEmpty) {
        event.eventType match {

          case "TASK_ASSIGNMENT" =>
            notificationActor ! SendTaskNotification(
              userEmail,
              event.taskId.getOrElse(0).toLong,
              event.description.getOrElse(""),
              event.eventName.getOrElse(""),
              event.eventDate.getOrElse(""),
              event.scheduledTime.getOrElse(""),
              event.specialRequest.getOrElse("")
            )

          case "STATUS_UPDATE" | "PROGRESS_CHECK" =>
            notificationActor ! SendTaskStatusUpdate(
              userEmail,
              event.taskId.getOrElse(0).toLong,
              event.description.getOrElse(""),
              event.eventName.getOrElse(""),
              event.eventDate.getOrElse(""),
              event.oldStatus.getOrElse(""),
              event.newStatus.getOrElse("")
            )

          case "REMINDER" =>
            notificationActor ! SendTaskReminder(
              userEmail,
              event.taskId.getOrElse(0).toLong,
              event.description.getOrElse(""),
              event.eventName.getOrElse(""),
              event.eventDate.getOrElse(""),
              event.scheduledTime.getOrElse("")
            )


          case "EVENT_DAY_ALERT" =>
            notificationActor ! SendEventAlert(
              userEmail,
              event.taskId.getOrElse(0).toLong,
              event.eventName.getOrElse(""),
              event.eventDate.getOrElse(""),
              "Event day reminder!"
            )

          case unknown =>
            log.warning(s"[TaskActor] Unknown eventType: $unknown")
        }
      } else {
        log.warning("[TaskActor] No userEmail found for event, skipping notification")
      }
  }
}
