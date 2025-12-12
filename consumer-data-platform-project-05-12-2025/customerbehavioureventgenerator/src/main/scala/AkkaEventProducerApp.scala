import akka.actor.{ActorSystem, Props}
import com.typesafe.scalalogging.LazyLogging
import config.AppConfig
import producer.KafkaEventProducer
import actor.BehaviouralEventGeneratorActor
import scheduler.EventScheduler
import repository.ReferenceDataRepository
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.avro.Schema
import java.util.Properties
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Success, Failure}

object AkkaEventProducerApp extends LazyLogging {

  def main(args: Array[String]): Unit = {

    logger.info("╔════════════════════════════════════════════════════════════╗")
    logger.info("║   Akka Behavioural Event Generator for Kafka              ║")
    logger.info("╚════════════════════════════════════════════════════════════╝")

    implicit val system: ActorSystem = ActorSystem("event-generator-system")

    try {
      logger.info("Phase 1: Loading configuration")
      val eventsPerSecond = AppConfig.akka.eventGenerator.eventsPerSecond
      val kafkaTopic = AppConfig.kafka.topic
      val kafkaServers = AppConfig.kafka.bootstrapServers
      logger.info(s"  Events/sec: $eventsPerSecond")
      logger.info(s"  Kafka topic: $kafkaTopic")
      logger.info(s"  Kafka brokers: $kafkaServers")

      logger.info("Phase 2: Loading reference data from MySQL")
      val repo = new ReferenceDataRepository()
      val customerIds = repo.loadCustomerIds()
      val productIds = repo.loadProductIds()

      if (customerIds.isEmpty || productIds.isEmpty) {
        logger.error("ERROR: Failed to load reference data. Cannot proceed.")
        system.terminate()
        return
      }

      logger.info("Phase 3: Initializing Kafka producer")
      val kafkaProducer = new KafkaProducer[String, Array[Byte]](producerProps)
      val eventProducer = new KafkaEventProducer(kafkaProducer, kafkaTopic)

      logger.info("Phase 4: Parsing Avro schema")
      val schema = new Schema.Parser().parse(avroSchema)

      logger.info("Phase 5: Creating actor system")
      val generatorActor = system.actorOf(
        Props(new BehaviouralEventGeneratorActor(customerIds, productIds, schema, eventProducer)),
        "behavioural-event-generator"
      )

      logger.info("Phase 6: Starting event scheduler")
      new EventScheduler(system, generatorActor).start(eventsPerSecond)

      logger.info("╔════════════════════════════════════════════════════════════╗")
      logger.info("║                                                            ║")
      logger.info("║  ✓ Event Generator is RUNNING                             ║")
      logger.info(s"║    - Generating $eventsPerSecond events/second             ║")
      logger.info(s"║    - Kafka topic: $kafkaTopic                 ║")
      logger.info(s"║    - Customers: ${customerIds.size} | Products: ${productIds.size}                    ║")
      logger.info("║                                                            ║")
      logger.info("║  Press Ctrl+C to stop                                     ║")
      logger.info("║                                                            ║")
      logger.info("╚════════════════════════════════════════════════════════════╝")

      // Block until terminated
      Await.result(system.whenTerminated, Duration.Inf)

    } catch {
      case e: Exception =>
        logger.error(s"FATAL ERROR: ${e.getMessage}", e)
        system.terminate()
    }
  }

  private def producerProps: Properties = {
    val props = new Properties()
    props.put("bootstrap.servers", AppConfig.kafka.bootstrapServers)
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")
    props.put("acks", "1")
    props.put("linger.ms", "5")
    props.put("batch.size", "32768")
    props.put("compression.type", "snappy")
    props.put("retries", "3")
    props.put("max.in.flight.requests.per.connection", "5")
    props
  }

  // Avro schema for behavioural events
  private val avroSchema: String =
    """
    {
      "type":"record",
      "name":"BehaviouralEvent",
      "namespace":"com.retail.events",
      "fields":[
        {
          "name":"event_id",
          "type":"string",
          "doc":"Unique event identifier (UUID)"
        },
        {
          "name":"customer_id",
          "type":"int",
          "doc":"Customer ID from MySQL customers table"
        },
        {
          "name":"product_id",
          "type":"int",
          "doc":"Product ID from MySQL products table"
        },
        {
          "name":"event_type",
          "type":"string",
          "doc":"Event type: LIKE, WISHLIST, or CART_ADD"
        },
        {
          "name":"event_timestamp",
          "type":"long",
          "doc":"Event creation timestamp in milliseconds"
        }
      ]
    }
    """
}