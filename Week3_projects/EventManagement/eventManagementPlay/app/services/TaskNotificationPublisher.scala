package services

import org.apache.pekko.actor.{ActorSystem, Cancellable}
import play.api.inject.ApplicationLifecycle
import play.api.libs.json.Json
import play.api.{Configuration, Logging}
import repositories.{EventUserRepository, TaskRepository, TeamUserRepository}

import java.sql.Timestamp
import javax.inject._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaskNotificationPublisher @Inject()(
                                           systemProvider: javax.inject.Provider[ActorSystem],
                                           lifecycle: ApplicationLifecycle,
                                           config: Configuration,
                                           taskRepo: TaskRepository,
                                           userRepo: EventUserRepository,
                                           teamUserRepo: TeamUserRepository,
                                           kafkaProducer: KafkaProducerService
                                         )(implicit ec: ExecutionContext) extends Logging {

  private lazy val system: ActorSystem = systemProvider.get()

  private val initialDelay: FiniteDuration =
    config.getOptional[FiniteDuration]("task.scheduler.initialDelay").getOrElse(2.seconds)
  private val interval: FiniteDuration =
    config.getOptional[FiniteDuration]("task.scheduler.interval").getOrElse(10.seconds)

  private val cancellable: Cancellable =
    system.scheduler.scheduleAtFixedRate(initialDelay, interval)(
      new Runnable {
        override def run(): Unit = {
          try {
            checkAndSendReminders()
            checkAndSendProgress()
            checkAndSendEventDayAlerts()
          } catch {
            case ex: Throwable => logger.error("TaskNotificationPublisher scheduler error", ex)
          }
        }
      }
    )(ec)

  lifecycle.addStopHook(() => Future { cancellable.cancel() })

  private def checkAndSendReminders(): Unit = {
    val now = new Timestamp(System.currentTimeMillis())

    val oneDayMs = 24 * 60 * 60 * 1000L
    val reminderTarget = new Timestamp(now.getTime + oneDayMs)

    // Allow small window around the exact reminder time
    val windowStart = new Timestamp(reminderTarget.getTime - interval.toMillis)
    val windowEnd   = new Timestamp(reminderTarget.getTime + interval.toMillis)

    taskRepo.getTasksWithin(windowStart, windowEnd).map { tasks =>
      tasks.foreach(sendReminder)
    }.recover { case ex =>
      logger.error("Reminder check failed", ex)
    }
  }
  private def sendReminder(task: models.Task): Unit = {
    val now = new Timestamp(System.currentTimeMillis())
    teamUserRepo.getEmailsByTeamId(task.teamId).foreach { emails =>
      emails.foreach { email =>
        val payload = Json.obj(
          "eventType" -> "REMINDER",
          "taskId" -> task.id,
          "eventId" -> task.eventId,
          "description" -> task.description,
          "userEmail" -> email,
          "scheduledTime" -> task.startTime.toString,
          "notifiedAt" -> now.toString
        )
        kafkaProducer.sendEvent("REMINDER", payload.toString())
      }
    }
  }

  private def checkAndSendProgress(): Unit = {
    taskRepo.getAllInProgressTasks.map { tasks =>
      tasks.foreach { task =>
        teamUserRepo.getEmailsByTeamId(task.teamId).foreach { emails =>
          emails.foreach { email =>
            val payload = Json.obj(
              "eventType" -> "PROGRESS_CHECK",
              "taskId" -> task.id,
              "eventId" -> task.eventId,
              "description" -> task.description,
              "userEmail" -> email
            )
            kafkaProducer.sendEvent("PROGRESS_CHECK", payload.toString())
          }
        }
      }
    }.recover { case ex => logger.error("Failed fetching in-progress tasks", ex) }
  }

  private def checkAndSendEventDayAlerts(): Unit = {
    taskRepo.getTasksForToday.map { tasks =>
      tasks.foreach { task =>
        teamUserRepo.getEmailsByTeamId(task.teamId).foreach { emails =>
          emails.foreach { email =>
            val payload = Json.obj(
              "eventType" -> "EVENT_DAY_ALERT",
              "taskId" -> task.id,
              "eventId" -> task.eventId,
              "description" -> task.description,
              "userEmail" -> email
            )
            kafkaProducer.sendEvent("EVENT_DAY_ALERT", payload.toString())
          }
        }

        userRepo.getEventManagerEmail().foreach {
          case Some(managerEmail) =>
            val payload = Json.obj(
              "eventType" -> "EVENT_DAY_ALERT",
              "taskId" -> task.id,
              "eventId" -> task.eventId,
              "description" -> task.description,
              "userEmail" -> managerEmail
            )
            kafkaProducer.sendEvent("EVENT_DAY_ALERT", payload.toString())
          case None =>
            logger.warn(s"No event manager found for task=${task.id}")
        }
      }
    }.recover { case ex => logger.error("Event-day alert check failed", ex) }
  }
}
