import akka.actor.{ActorSystem, Props}
import actors._


object Main extends App {

  implicit val system: ActorSystem = ActorSystem("EquipmentSystem")
  implicit val ec = system.dispatcher

  val inventoryActor = system.actorOf(Props[InventoryActor], "inventoryActor")
  val maintenanceActor = system.actorOf(Props[MaintenanceActor], "maintenanceActor")
  val notificationActor = system.actorOf(Props[NotificationActor], "notificationActor")

  val kafkaConsumerActor = system.actorOf(
    KafkaConsumerActor.props(inventoryActor, maintenanceActor, notificationActor),
    "kafkaConsumerActor"
  )
}
