package services

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import javax.inject._
import java.util.Properties

@Singleton
class KafkaProducerService {

  private val props = new Properties()
  props.put("bootstrap.servers", "localhost:9092")
  props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

  private val producer = new KafkaProducer[String, String](props)

  def sendEvent(eventType: String, payload: String): Unit = {
    val record = new ProducerRecord[String, String]("event-notifications", eventType, payload)
    producer.send(record)
  }
}
