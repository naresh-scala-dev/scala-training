package actors

import akka.actor.Actor
import service.EmailService

class NotificationActor extends Actor {

  private val emailService = new EmailService()

  override def receive: Receive = {

    case NotificationActor.SendOverdueReminder(employeeEmail, equipmentId, expectedReturn, condition) =>
      val subject = "Overdue Equipment Reminder"
      val body =
        s"Dear Employee,\n\nYour allocated equipment with ID $equipmentId is overdue. Expected return: $expectedReturn, Condition: $condition.\n\nPlease return it immediately.\n\nThank you."
      emailService.sendEmail(Some("noreply@company.com"), employeeEmail, subject, body)

    case NotificationActor.SendMaintenanceAlert(maintenanceEmail, equipmentId) =>
      val subject = "Damaged Equipment Alert"
      val body =
        s"Dear Maintenance Team,\n\nEquipment with ID $equipmentId has been returned damaged. Please inspect and repair.\n\nThank you."
      emailService.sendEmail(Some("noreply@company.com"), maintenanceEmail, subject, body)

    case NotificationActor.SendInventoryUpdate(inventoryEmail, equipmentId, eventType) =>
      val subject = s"Equipment $eventType Notification"
      val body =
        s"Dear Inventory Team,\n\nEquipment ID $equipmentId has been $eventType. Please update your records.\n\nThank you."
      emailService.sendEmail(Some("noreply@company.com"), inventoryEmail, subject, body)

    case NotificationActor.SendAllocationNotification(employeeEmail, inventoryEmail, equipmentId) =>
      emailService.sendEmail(Some("noreply@company.com"), employeeEmail, "Equipment Allocated",
        s"Dear Employee,\n\nEquipment ID $equipmentId has been allocated to you.\n\nThank you.")
      emailService.sendEmail(Some("noreply@company.com"), inventoryEmail, s"Equipment Allocated (ID: $equipmentId)",
        s"Dear Inventory Team,\n\nEquipment ID $equipmentId has been allocated.\n\nThank you.")

    case NotificationActor.SendReturnNotification(employeeEmail, inventoryEmail, equipmentId, condition) =>
      emailService.sendEmail(Some("noreply@company.com"), employeeEmail, "Equipment Returned",
        s"Dear Employee,\n\nEquipment ID $equipmentId has been returned.\nCondition: $condition\n\nThank you.")
      emailService.sendEmail(Some("noreply@company.com"), inventoryEmail, s"Equipment Returned (ID: $equipmentId)",
        s"Dear Inventory Team,\n\nEquipment ID $equipmentId has been returned.\nCondition: $condition\n\nThank you.")
  }
}

object NotificationActor {
  case class SendOverdueReminder(employeeEmail: String, equipmentId: Long, expectedReturn: String, condition: String)
  case class SendMaintenanceAlert(maintenanceEmail: String, equipmentId: Long)
  case class SendInventoryUpdate(inventoryEmail: String, equipmentId: Long, eventType: String)
  case class SendAllocationNotification(employeeEmail: String, inventoryEmail: String, equipmentId: Long)
  case class SendReturnNotification(employeeEmail: String, inventoryEmail: String, equipmentId: Long, condition: String)
}
