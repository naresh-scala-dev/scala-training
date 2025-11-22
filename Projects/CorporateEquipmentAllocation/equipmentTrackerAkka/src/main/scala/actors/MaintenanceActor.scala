package actors

import akka.actor.{Actor, ActorLogging, Props}
import actors.NotificationActor.SendInventoryUpdate

object MaintenanceActor {
  def props: Props = Props[MaintenanceActor]
  case class Damaged(id: Long, inventoryEmail: Option[String] = None)
  case class Repaired(id: Long, inventoryEmail: Option[String] = None)
}

class MaintenanceActor extends Actor with ActorLogging {
  import MaintenanceActor._
  var underRepair = Set.empty[Long]

  override def receive: Receive = {
    case Damaged(id, inventoryEmail) =>
      underRepair += id
      inventoryEmail.foreach(email => context.actorSelection("/user/notificationActor") ! SendInventoryUpdate(email, id, "damaged"))

    case Repaired(id, inventoryEmail) =>
      if (underRepair.contains(id)) {
        underRepair -= id
        inventoryEmail.foreach(email => context.actorSelection("/user/notificationActor") ! SendInventoryUpdate(email, id, "repaired"))
      }
  }
}
