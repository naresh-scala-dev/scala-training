//package actor
//
//import akka.actor.Actor
//import com.typesafe.scalalogging.LazyLogging
//import producer.KafkaEventProducer
//import org.apache.avro.Schema
//import org.apache.avro.generic.{GenericData, GenericRecord}
//import org.apache.avro.io.EncoderFactory
//import org.apache.avro.generic.GenericDatumWriter
//import java.io.ByteArrayOutputStream
//import java.util.UUID
//import scala.util.Random
//
//class EventGeneratorActor(
//                           customerIds: Seq[Int],
//                           productIds: Seq[Int],
//                           schema: Schema,
//                           kafkaProducer: KafkaEventProducer
//                         ) extends Actor with LazyLogging {
//
//  private val random = new Random()
//  private val eventTypes = Seq("LIKE", "WISHLIST", "CART_ADD")
//
//  override def preStart(): Unit = logger.info("EventGeneratorActor started")
//
//  override def receive: Receive = {
//    case "GENERATE_EVENT" =>
//      val avroBytes = generateAvroEvent()
//      kafkaProducer.publish(UUID.randomUUID().toString, avroBytes)
//  }
//
//  private def generateAvroEvent(): Array[Byte] = {
//    val now = System.currentTimeMillis()
//    val record: GenericRecord = new GenericData.Record(schema)
//
//    record.put("event_id", UUID.randomUUID().toString)
//    record.put("customer_id", customerIds(random.nextInt(customerIds.size)))
//    record.put("product_id", productIds(random.nextInt(productIds.size)))
//    record.put("event_type", eventTypes(random.nextInt(eventTypes.size)))
//    record.put("event_timestamp", now)
//    record.put("ingestion_timestamp", now)  // Add this
//
//    val out = new ByteArrayOutputStream()
//    val writer = new GenericDatumWriter[GenericRecord](schema)
//    val encoder = EncoderFactory.get().binaryEncoder(out, null)
//    writer.write(record, encoder)
//    encoder.flush()
//    out.close()
//
//    logger.debug(s"Generated Avro event: event_id=${record.get("event_id")} customer_id=${record.get("customer_id")} product_id=${record.get("product_id")}")
//    out.toByteArray
//  }
//}