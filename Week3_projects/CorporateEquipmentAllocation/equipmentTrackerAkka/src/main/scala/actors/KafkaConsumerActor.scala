package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import org.apache.kafka.clients.consumer.KafkaConsumer
import play.api.libs.json._
import scala.jdk.CollectionConverters._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import actors.NotificationActor._

object KafkaConsumerActor {
  def props(
             inventoryActor: ActorRef,
             maintenanceActor: ActorRef,
             notificationActor: ActorRef
           ): Props =
    Props(new KafkaConsumerActor(inventoryActor, maintenanceActor, notificationActor))

  case object Poll
}

case class EquipmentEvent(
                           eventType: String,
                           equipmentId: Long,
                           employeeEmail: Option[String],
                           inventoryEmail: Option[String],
                           maintenanceEmail: Option[String]
                         )

class KafkaConsumerActor(
                          inventoryActor: ActorRef,
                          maintenanceActor: ActorRef,
                          notificationActor: ActorRef
                        ) extends Actor with ActorLogging {

  implicit val ec: ExecutionContext = context.dispatcher
  implicit val format: Format[EquipmentEvent] = Json.format[EquipmentEvent]

  private val props = new java.util.Properties()
  props.put("bootstrap.servers", "localhost:9092")
  props.put("group.id", "equipment-service")
  props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
  props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")

  private val consumer = new KafkaConsumer[String, String](props)
  consumer.subscribe(java.util.Collections.singletonList("equipment-events"))

  override def preStart(): Unit = self ! KafkaConsumerActor.Poll
  override def postStop(): Unit = consumer.close()

  override def receive: Receive = {
    case KafkaConsumerActor.Poll =>
      val records = consumer.poll(java.time.Duration.ofMillis(500))

      for (record <- records.asScala) {
        try {
          val json = Json.parse(record.value())
          json.validate[EquipmentEvent] match {

            case JsSuccess(event, _) =>
              log.info(s"[KafkaConsumerActor] Received: $event")

              val emp = event.employeeEmail.getOrElse("")
              val inv = event.inventoryEmail.getOrElse("")
              val main = event.maintenanceEmail.getOrElse("")

              event.eventType match {

                case "allocated" =>
                  inventoryActor ! InventoryActor.Allocated(event.equipmentId)
                  if (emp.nonEmpty && inv.nonEmpty)
                    notificationActor ! SendAllocationNotification(emp, inv, event.equipmentId)
                  else if (emp.nonEmpty)
                    notificationActor ! SendAllocationNotification(emp, "", event.equipmentId)
                  else if (inv.nonEmpty)
                    notificationActor ! SendInventoryUpdate(inv, event.equipmentId, "allocated")

                case "returned" =>
                  inventoryActor ! InventoryActor.Returned(event.equipmentId)
                  if (emp.nonEmpty && inv.nonEmpty)
                    notificationActor ! SendReturnNotification(emp, inv, event.equipmentId, "OK")
                  else if (emp.nonEmpty)
                    notificationActor ! SendReturnNotification(emp, "", event.equipmentId, "OK")
                  else if (inv.nonEmpty)
                    notificationActor ! SendInventoryUpdate(inv, event.equipmentId, "returned")

                case "damaged" =>
                  maintenanceActor ! MaintenanceActor.Damaged(event.equipmentId)
                  if (main.nonEmpty)
                    notificationActor ! SendMaintenanceAlert(main, event.equipmentId)
                  if (inv.nonEmpty)
                    notificationActor ! SendInventoryUpdate(inv, event.equipmentId, "damaged")

                case "repaired" =>
                  inventoryActor ! InventoryActor.Repaired(event.equipmentId)
                  if (inv.nonEmpty)
                    notificationActor ! SendInventoryUpdate(inv, event.equipmentId, "repaired & available")

                case "overdue" =>
                  val json = Json.parse(record.value())
                  val expectedReturnStr = (json \ "expectedReturn").asOpt[String].getOrElse("")
                  val condition = (json \ "condition").asOpt[String].getOrElse("")
                  val allocationId = (json \ "allocationId").asOpt[Long].getOrElse(0L)
                  val emp = (json \ "userEmail").asOpt[String].getOrElse("")

                  if (emp.nonEmpty) {
                    println(s"Sending email to $emp for allocation $allocationId")
                    notificationActor ! NotificationActor.SendOverdueReminder(emp, json("equipmentId").as[Long], expectedReturnStr, if (condition.nonEmpty) condition else "Not specified")
                  } else {
                    log.warning(s"Overdue event without employeeEmail: ${record.value()}")
                  }

                case other =>
                  log.warning(s"Unknown event type: $other")
              }

            case JsError(err) =>
              log.error(s"Invalid event JSON: ${record.value()} | $err")
          }

        } catch {
          case e: Exception =>
            log.error(s"Failed to process event: $e")
        }
      }

      context.system.scheduler.scheduleOnce(500.milliseconds, self, KafkaConsumerActor.Poll)
  }
}
