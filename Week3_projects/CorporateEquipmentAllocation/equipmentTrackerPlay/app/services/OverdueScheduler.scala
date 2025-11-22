package services

import org.apache.pekko.actor.{ActorSystem, Cancellable}
import play.api.inject.ApplicationLifecycle
import play.api.libs.json.Json
import play.api.{Configuration, Logging}
import repositories.{AllocationRepository, UserRepository}

import java.sql.Timestamp
import javax.inject._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OverdueScheduler @Inject()(
                                  systemProvider: javax.inject.Provider[ActorSystem],
                                  lifecycle: ApplicationLifecycle,
                                  config: Configuration,
                                  allocationRepo: AllocationRepository,
                                  userRepo: UserRepository,
                                  kafkaProducer: KafkaProducerService
                                )(implicit ec: ExecutionContext) extends Logging {

  private lazy val system: ActorSystem = systemProvider.get()

  private val initialDelay: FiniteDuration =
    config.getOptional[FiniteDuration]("overdue.scheduler.initialDelay").getOrElse(2.seconds)
  private val interval: FiniteDuration =
    config.getOptional[FiniteDuration]("overdue.scheduler.interval").getOrElse(10.seconds)

  logger.info(s"OverdueScheduler initializing (initialDelay=$initialDelay, interval=$interval)")


  private val cancellable: Cancellable =
    system.scheduler.scheduleAtFixedRate(initialDelay, interval)(
      new Runnable {
        override def run(): Unit = {
          try checkAndSendOverdueReminders()
          catch {
            case ex: Throwable => logger.error("OverdueScheduler run error", ex)
          }
        }
      }
    )(ec)

  lifecycle.addStopHook { () =>
    Future {
      logger.info("Stopping OverdueScheduler")
      cancellable.cancel()
    }
  }

  /** Core logic: check DB for overdue allocations and send reminder events */
  private def checkAndSendOverdueReminders(): Unit = {
    val now = new Timestamp(System.currentTimeMillis())

    allocationRepo.getOverdue(now).map { overdueSeq =>
      if (overdueSeq.isEmpty) {
        logger.info("No overdue allocations — nothing to do")
      } else {
        logger.info(s"Found ${overdueSeq.size} overdue allocations — sending reminders")
        Future.sequence(overdueSeq.map(sendReminderForAllocation)).map(_ => ())
      }
    }.recover {
      case ex: java.sql.SQLTransientConnectionException =>
        logger.error("DB connection unavailable while checking overdue allocations", ex)
      case ex: Throwable =>
        logger.error("Unexpected error while checking overdue allocations", ex)
    }
  }

  /** Send reminder for a single allocation including user email */
  private def sendReminderForAllocation(alloc: models.Allocation): Future[Unit] = {
    val now = new Timestamp(System.currentTimeMillis())

    userRepo.getEmailById(alloc.userId).flatMap {
      case Some(email) =>
        val payload = Json.obj(
          "eventType" -> "overdue",
          "allocationId" -> alloc.id,
          "equipmentId" -> alloc.equipmentId,
          "userId" -> alloc.userId,
          "userEmail" -> email,
          "expectedReturn" -> alloc.expectedReturn.toString,
          "notifiedAt" -> now.toString
        )

        val kafkaFuture: Future[Unit] = Future {
          kafkaProducer.sendEvent("overdue", payload.toString())
        }

        val dbFuture: Future[Unit] = allocationRepo.markReminderSent(alloc.id).map(_ => ())

        for {
          _ <- kafkaFuture
          _ <- dbFuture
        } yield ()

      case None =>
        logger.warn(s"No email found for userId=${alloc.userId}, skipping Kafka event")
        allocationRepo.markReminderSent(alloc.id).map(_ => ())
    }.recover { case ex =>
      logger.error(s"Failed to send reminder for allocationId=${alloc.id}", ex)
    }
  }


}
