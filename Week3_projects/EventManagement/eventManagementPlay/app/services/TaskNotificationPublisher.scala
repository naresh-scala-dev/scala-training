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
                                         )(implicit ec: ExecutionContext)
  extends Logging {

  private lazy val system: ActorSystem = systemProvider.get()

  private val initialDelay: FiniteDuration =
    config.getOptional[FiniteDuration]("task.scheduler.initialDelay").getOrElse(2.seconds)
  private val interval: FiniteDuration =
    config.getOptional[FiniteDuration]("task.scheduler.interval").getOrElse(10.seconds)

  logger.info(s"[TaskScheduler] Started (initialDelay=$initialDelay, interval=$interval)")

  private val cancellable: Cancellable =
    system.scheduler.scheduleAtFixedRate(initialDelay, interval)(
      new Runnable {
        override def run(): Unit = {
          try {
            checkAndSendReminders()
            checkAndSendProgress()
            checkAndSendEventDayAlerts()
          } catch {
            case ex: Throwable =>
              logger.error("[TaskScheduler] Unexpected scheduler error", ex)
          }
        }
      }
    )(ec)

  lifecycle.addStopHook(() => Future { cancellable.cancel() })

  private def checkAndSendReminders(): Unit = {
    val now = new Timestamp(System.currentTimeMillis())
    val oneDayMs = 24 * 60 * 60 * 1000L      // REAL 1 day before start

    val reminderTarget = new Timestamp(now.getTime + oneDayMs)
    val windowStart = new Timestamp(reminderTarget.getTime - interval.toMillis)
    val windowEnd   = new Timestamp(reminderTarget.getTime + interval.toMillis)

    logger.info(s"[REMINDER] Checking tasks between $windowStart and $windowEnd")

    taskRepo.getPendingReminders(windowStart, windowEnd).map { tasks =>
      if (tasks.isEmpty) {
        logger.info("[REMINDER] No tasks matched reminder window.")
      }

      tasks.foreach { task =>
        logger.info(s"[REMINDER] -> Task ${task.id} matched. Sending notifications.")

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

        taskRepo.markReminderSent(task.id)
      }
    }
  }

  private def checkAndSendProgress(): Unit = {
    logger.info("[PROGRESS] Checking all InProgress tasks...")

    taskRepo.getAllInProgressTasks.map { tasks =>
      if (tasks.isEmpty) logger.info("[PROGRESS] No InProgress tasks found.")

      tasks.foreach { task =>
        logger.info(s"[PROGRESS] -> Task ${task.id} is InProgress. Publishing message.")

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
    }
  }


  private def checkAndSendEventDayAlerts(): Unit = {
    logger.info("[EVENT DAY ALERT] Checking tasks scheduled for today...")

    taskRepo.getPendingEventDayAlerts().map { tasks =>
      if (tasks.isEmpty) {
        logger.info("[EVENT DAY ALERT] No tasks for today or already alerted.")
      }

      tasks.foreach { task =>
        logger.info(s"[EVENT DAY ALERT] -> Sending alerts for Task ${task.id}")

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
            kafkaProducer.sendEvent("EVENT_DAY_ALERT",
              Json.obj(
                "eventType" -> "EVENT_DAY_ALERT",
                "taskId" -> task.id,
                "eventId" -> task.eventId,
                "description" -> task.description,
                "userEmail" -> managerEmail
              ).toString()
            )
          case None =>
            logger.warn(s"[EVENT DAY ALERT] No event manager found for task ${task.id}")
        }

        taskRepo.markEventDayAlertSent(task.id)
      }
    }
  }
}
