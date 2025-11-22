package services

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import java.util.Properties
import javax.inject.Singleton

@Singleton
class KafkaProducerService {

  private val props = new Properties()
  props.put("bootstrap.servers", "localhost:9092")
  props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

  private val producer = new KafkaProducer[String, String](props)

  /** Allocation event (requires both emails) */
  def sendEvent(eventType: String, equipmentId: Long, employeeEmail: String, inventoryEmail: String): Unit = {

    val json =
      s"""{
         | "eventType": "$eventType",
         | "equipmentId": $equipmentId,
         | "employeeEmail": "$employeeEmail",
         | "inventoryEmail": "$inventoryEmail"
         |}""".stripMargin

    producer.send(
      new ProducerRecord[String, String]("equipment-events", eventType, json)
    )
  }

  /** Overdue event (no emails) */
  def sendEvent(eventType: String, jsonPayload: String): Unit = {
    println(s" Sending Kafka Overdue Event: $jsonPayload")

    producer.send(
      new ProducerRecord[String, String]("equipment-events", eventType, jsonPayload)
    )
  }
}
