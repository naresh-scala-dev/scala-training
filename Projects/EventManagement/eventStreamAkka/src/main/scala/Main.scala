import akka.actor.ActorSystem
import actors._
import service.EmailService
import com.typesafe.config.ConfigFactory

object Main extends App {

  implicit val system: ActorSystem = ActorSystem("EventSystem")
  implicit val ec = system.dispatcher

  val config = ConfigFactory.load()
  val emailService = new EmailService(config)

  val notificationActor = system.actorOf(NotificationActor.props(emailService), "notificationActor")
  val taskActor = system.actorOf(TaskActor.props(notificationActor), "taskActor")
  val eventActor = system.actorOf(EventActor.props(notificationActor), "eventActor")
  val kafkaConsumerActor = system.actorOf(KafkaConsumerActor.props(taskActor, eventActor, notificationActor), "kafkaConsumerActor")

  println("[Main] System initialized. Kafka consumer running.")

  sys.addShutdownHook {
    println("[Main] Shutting down EventSystem...")
    system.terminate()
  }

  system.whenTerminated.foreach(_ => println("[Main] Actor system terminated."))
}
