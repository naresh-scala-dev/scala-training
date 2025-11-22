package actors

import akka.actor.{Actor, ActorLogging}

object InventoryActor {
  def props = akka.actor.Props[InventoryActor]
  case class Allocated(id: Long)
  case class Returned(id: Long)
  case class Damaged(id: Long)
  case class Repaired(id: Long)
}

class InventoryActor extends Actor with ActorLogging {
  var equipmentStatus = Map.empty[Long, String]

  override def receive: Receive = {
    case InventoryActor.Allocated(id) =>
      equipmentStatus += id -> "allocated"
    case InventoryActor.Returned(id) =>
      equipmentStatus += id -> "available"
    case InventoryActor.Damaged(id) =>
      equipmentStatus += id -> "damaged"
    case InventoryActor.Repaired(id) =>
      equipmentStatus += id -> "available"
    case "print" =>
      log.info(s"Status: $equipmentStatus")
  }
}
