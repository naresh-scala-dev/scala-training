package scheduler

import akka.actor.{ActorRef, ActorSystem}
import com.typesafe.scalalogging.LazyLogging
import scala.concurrent.duration._

class EventScheduler(
                      system: ActorSystem,
                      actor: ActorRef
                    ) extends LazyLogging {

  def start(eventsPerSecond: Int): Unit = {
    require(eventsPerSecond > 0, "eventsPerSecond must be > 0")

    val interval = (1000 / eventsPerSecond).millis
    logger.info(s"=== EventScheduler Started ===")
    logger.info(s"Event rate: $eventsPerSecond events/second")
    logger.info(s"Interval: ${interval.toMillis}ms")

    system.scheduler.scheduleAtFixedRate(
      initialDelay = 1.second,
      interval = interval,
      receiver = actor,
      message = "GENERATE_EVENT"
    )(system.dispatcher)
  }
}
