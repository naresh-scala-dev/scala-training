package actors

import actors.EventActor.HandleEventAlert
import akka.actor.{Actor, ActorLogging, Props}
import service.TaskEvent

object EventActor {
  def props(notificationActor: akka.actor.ActorRef): Props =
    Props(new EventActor(notificationActor))

  case class HandleEventAlert(event: TaskEvent)
}

class EventActor(notificationActor: akka.actor.ActorRef)
  extends Actor with ActorLogging {

  import NotificationActor._

  override def receive: Receive = {
    case HandleEventAlert(event) =>
      log.info(s"[EventActor] Sending final day alert for event ${event.eventId.getOrElse(0)}")

      val eventName = event.eventName.getOrElse("Unnamed Event")
      val eventDate = event.eventDate.map(_.toString).getOrElse("Unknown Date")

      val message =
        s"""Hello Team,
           |
           |This is a final day alert for your event:
           |---------------------------
           |Event: $eventName
           |Date: $eventDate
           |---------------------------
           |
           |Please ensure all preparations are complete and tasks are finished.
           |
           |Regards,
           |Event Manager
           |""".stripMargin

      // Send to each assigned user
      event.userEmail.foreach { userEmail =>
        notificationActor ! SendEventAlert(
          to = userEmail,
          eventId = event.eventId.getOrElse(0).toLong,
          eventName = eventName,
          eventDate = eventDate,
          message = message
        )
      }
  }
}
