package actors

import akka.actor.Actor
import service.EmailService

object NotificationActor {

  case class SendTaskNotification(
                                   userEmail: String,
                                   taskId: Long,
                                   taskDescription: String,
                                   eventName: String,
                                   eventDate: String,
                                   scheduledTime: String,
                                   specialRequest: String
                                 )

  case class SendTaskStatusUpdate(
                                   userEmail: String,
                                   taskId: Long,
                                   taskDescription: String,
                                   eventName: String,
                                   eventDate: String,
                                   oldStatus: String,
                                   newStatus: String
                                 )

  case class SendTaskReminder(
                               userEmail: String,
                               taskId: Long,
                               taskDescription: String,
                               eventName: String,
                               eventDate: String,
                               scheduledTime: String
                             )

  case class SendEventAlert(
                             to: String,
                             eventId: Long,
                             eventName: String,
                             eventDate: String,
                             message: String
                           )

  def props(emailService: EmailService) =
    akka.actor.Props(new NotificationActor(emailService))
}

class NotificationActor(emailService: EmailService) extends Actor {
  import NotificationActor._

  override def receive: Receive = {

    case SendTaskNotification(userEmail, taskId, taskDesc, eventName, eventDate, scheduledTime, specialRequest) =>
      val subject = s"[EventSystem] Task Assignment: $taskDesc"
      val body =
        s"""Hello Team,
           |
           |You have been assigned the following task for the upcoming event:
           |
           |- Event: $eventName
           |- Date: $eventDate
           |- Task: $taskDesc
           |${if (specialRequest.nonEmpty) s"- Special Request: $specialRequest" else ""}
           |- Scheduled Time: $scheduledTime
           |
           |Please ensure everything is prepared before the event starts.
           |
           |Regards,
           |Event Manager
           |""".stripMargin
      emailService.sendEmail(to = userEmail, subject = subject, body = body)
      println(s">>> Task Assignment email sent to $userEmail")

    case SendTaskStatusUpdate(userEmail, taskId, taskDesc, eventName, eventDate, oldStatus, newStatus) =>
      val subject = s"[EventSystem] Task Status Update: $taskDesc"
      val body =
        s"""Hello Team,
           |
           |Task Update for your assigned task:
           |--------------------------
           |Event: $eventName
           |Date: $eventDate
           |Task: $taskDesc
           |Status Change: $oldStatus → $newStatus
           |--------------------------
           |
           |Please take necessary action if required.
           |
           |Regards,
           |Event Manager
           |""".stripMargin
      emailService.sendEmail(to = userEmail, subject = subject, body = body)
      println(s">>> Task Status Update email sent to $userEmail")

    case SendTaskReminder(userEmail, taskId, taskDesc, eventName, eventDate, scheduledTime) =>
      val subject = s"[Reminder] Upcoming Task: $taskDesc"
      val body =
        s"""Hello Team,
           |
           |This is a friendly reminder for your upcoming task:
           |
           |- Event: $eventName
           |- Date: $eventDate
           |- Task: $taskDesc
           |- Scheduled Time: $scheduledTime

           |
           |Please make sure you are prepared.
           |
           |Regards,
           |Event Manager
           |""".stripMargin
      emailService.sendEmail(to = userEmail, subject = subject, body = body)
      println(s">>> Task Reminder email sent to $userEmail")

    case SendEventAlert(to, eventId, eventName, eventDate, message) =>
      val subject = s"[EventSystem] Event Alert: $eventName"
      val body =
        s"""Hello Team,
           |
           |Event Alert:
           |--------------------------
           |Event: $eventName
           |Date: $eventDate
           |Details: $message
           |--------------------------
           |
           |Please take necessary action.
           |
           |Regards,
           |Event Manager
           |""".stripMargin
      emailService.sendEmail(to = to, subject = subject, body = body)
      println(s">>> Event Alert email sent to $to")
  }
}
