package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import play.api.libs.json._
import scala.jdk.CollectionConverters._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import java.util.Properties
import org.apache.kafka.clients.consumer.KafkaConsumer
import service.TaskEvent

object KafkaConsumerActor {
  def props(taskActor: ActorRef, eventActor: ActorRef, notificationActor: ActorRef): Props =
    Props(new KafkaConsumerActor(taskActor, eventActor, notificationActor))

  case object Poll
}

class KafkaConsumerActor(
                          taskActor: ActorRef,
                          eventActor: ActorRef,
                          notificationActor: ActorRef
                        ) extends Actor with ActorLogging {

  implicit val ec: ExecutionContext = context.dispatcher
  import TaskEvent._

  private val bootstrapServers = "localhost:9092"
  private val groupId = "event-management"
  private val topic = "event-notifications"
  private val pollInterval = 500.millis

  private val props = new Properties()
  props.put("bootstrap.servers", bootstrapServers)
  props.put("group.id", groupId)
  props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
  props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
  props.put("auto.offset.reset", "latest")

  private val consumer = new KafkaConsumer[String, String](props)

  override def preStart(): Unit = {
    log.info(s"[KafkaConsumerActor] Starting consumer | Topic=$topic | Group=$groupId")
    consumer.subscribe(java.util.Collections.singletonList(topic))
    self ! KafkaConsumerActor.Poll
  }

  override def postStop(): Unit = {
    log.info("[KafkaConsumerActor] Consumer stopped")
    consumer.close()
  }

  override def receive: Receive = {
    case KafkaConsumerActor.Poll =>
      val records = consumer.poll(java.time.Duration.ofMillis(500))
      log.info(s"[KafkaConsumerActor] Polled ${records.count()} message(s)")

      for (record <- records.asScala) {
        try {
          val json = Json.parse(record.value())
          json.validate[TaskEvent] match {
            case JsSuccess(event, _) =>
              event.eventType match {
                case "TASK_ASSIGNMENT" | "STATUS_UPDATE" | "REMINDER" | "PROGRESS_CHECK" | "EVENT_DAY_ALERT" =>
                  taskActor ! TaskActor.HandleTaskEvent(event)
                    println("naresh******")
                case "FINAL_ALERT" =>
                  eventActor ! EventActor.HandleEventAlert(event)
                  println("nagula")
                case unknown =>
                  log.warning(s"Unknown eventType received: $unknown")
                  println("jdhbvjsdk")
              }
            case JsError(err) =>
              log.error(s"JSON parse failed: ${record.value()}, errors: $err")
          }
        } catch {
          case ex: Exception =>
            log.error(s"Error processing record: ${ex.getMessage}", ex)
        }
      }

      context.system.scheduler.scheduleOnce(pollInterval, self, KafkaConsumerActor.Poll)
  }
}
