package producer

import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

class KafkaEventProducer(
                          producer: KafkaProducer[String, Array[Byte]],
                          topic: String
                        ) extends LazyLogging {

  def publish(key: String, avroBytes: Array[Byte]): Unit = {
    val record = new ProducerRecord[String, Array[Byte]](topic, key, avroBytes)
    producer.send(record, (metadata, exception) => {
      if (exception != null) {
        logger.error(s"Failed to send event $key to Kafka", exception)
      } else {
        logger.debug(s"Sent event $key to topic=${metadata.topic()} partition=${metadata.partition()} offset=${metadata.offset()}")
      }
    })
  }

  def close(): Unit = {
    logger.info("Closing Kafka producer")
    producer.close()
  }
}