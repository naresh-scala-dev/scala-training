package actor

import akka.actor.Actor
import com.typesafe.scalalogging.LazyLogging
import producer.KafkaEventProducer
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericData, GenericRecord}
import org.apache.avro.io.EncoderFactory
import org.apache.avro.generic.GenericDatumWriter
import java.io.ByteArrayOutputStream
import java.util.UUID
import scala.util.Random

class BehaviouralEventGeneratorActor(
                                      customerIds: Seq[Int],
                                      productIds: Seq[Int],
                                      schema: Schema,
                                      kafkaProducer: KafkaEventProducer
                                    ) extends Actor with LazyLogging {

  private val random = new Random()
  private val eventTypes = Seq("LIKE", "WISHLIST", "CART_ADD")
  private var eventCount = 0L

  override def preStart(): Unit = {
    logger.info("=== BehaviouralEventGeneratorActor Started ===")
    logger.info(s"Customers available: ${customerIds.size}")
    logger.info(s"Products available: ${productIds.size}")
    logger.info(s"Event types: ${eventTypes.mkString(", ")}")
  }

  override def receive: Receive = {
    case "GENERATE_EVENT" =>
      generateAndPublishEvent()
      eventCount += 1
      if (eventCount % 100 == 0) {
        logger.info(s"Total events generated: $eventCount")
      }
  }

  private def generateAndPublishEvent(): Unit = {
    val eventBytes = generateAvroEvent()
    kafkaProducer.publish(UUID.randomUUID().toString, eventBytes)
  }

  private def generateAvroEvent(): Array[Byte] = {
    val eventId = UUID.randomUUID().toString
    val customerId = customerIds(random.nextInt(customerIds.size))
    val eventType = eventTypes(random.nextInt(eventTypes.size))
    val productId = productIds(random.nextInt(productIds.size))
    val eventTimestamp = System.currentTimeMillis()

    val record: GenericRecord = new GenericData.Record(schema)
    record.put("event_id", eventId)
    record.put("customer_id", customerId)
    record.put("product_id", productId)
    record.put("event_type", eventType)
    record.put("event_timestamp", eventTimestamp)

    val out = new ByteArrayOutputStream()
    val writer = new GenericDatumWriter[GenericRecord](schema)
    val encoder = EncoderFactory.get().binaryEncoder(out, null)
    writer.write(record, encoder)
    encoder.flush()
    out.close()

    logger.debug(s"Event#$eventCount: $eventId | customer=$customerId | type=$eventType | product=$productId | ts=$eventTimestamp")
    out.toByteArray
  }
}
