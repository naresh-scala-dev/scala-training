package protobuf

import com.google.protobuf.{DescriptorProtos, Descriptors, DynamicMessage}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}
import org.apache.log4j.Logger
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

import java.nio.file.{Files, Paths}
import java.util.Properties
import scala.util.{Random, Try}

object UserEventProcessor {

  var descriptorSet: DescriptorProtos.FileDescriptorSet = _
  var descriptor: Descriptors.Descriptor = _

  def loadDescriptorFile(path: String): Unit = {
    val logger = Logger.getLogger(getClass.getName)
    logger.info("\n=== STEP 1: LOADING DESCRIPTOR FILE ===")
    logger.info(s"Loading descriptor from: $path")

    val bytes = Files.readAllBytes(Paths.get(path))
    descriptorSet = DescriptorProtos.FileDescriptorSet.parseFrom(bytes)

    val fileDescriptor = descriptorSet.getFile(0)
    descriptor = Descriptors.FileDescriptor.buildFrom(
      fileDescriptor,
      Array[Descriptors.FileDescriptor]()
    ).getMessageTypes.get(0)

    logger.info(s"Descriptor message name: ${descriptor.getFullName}")
    logger.info(s"Message fields: userId (INT32), action (STRING), value (DOUBLE)")
    logger.info("Descriptor file loaded successfully\n")
  }

  def produceMessages(): Unit = {
    val logger = Logger.getLogger(getClass.getName)
    logger.info("\n=== STEP 2: PRODUCING 20 PROTOBUF MESSAGES ===")

    val props = new Properties()
    props.put("bootstrap.servers", "localhost:9092")
    props.put("key.serializer", classOf[StringSerializer].getName)
    props.put("value.serializer", classOf[ByteArraySerializer].getName)
    props.put("acks", "all")
    props.put("retries", "3")

    val producer = new KafkaProducer[String, Array[Byte]](props)
    val actions = Array("click", "view", "purchase", "add_to_cart", "remove_from_cart")
    val random = new Random()

    logger.info("Producing messages to Kafka topic: user-events\n")

    for (i <- 1 to 20) {
      val userId = random.nextInt(100) + 1
      val action = actions(random.nextInt(actions.length))
      val value = random.nextDouble() * 1000

      val messageBuilder = DynamicMessage.newBuilder(descriptor)
        .setField(descriptor.findFieldByName("userId"), userId)
        .setField(descriptor.findFieldByName("action"), action)
        .setField(descriptor.findFieldByName("value"), value)

      val userEvent = messageBuilder.build()
      val serializedEvent = userEvent.toByteArray()

      val record = new ProducerRecord[String, Array[Byte]](
        "user-events",
        userId.toString,
        serializedEvent
      )

      producer.send(record)

      logger.info(s"Message $i: userId=$userId, action=$action, value=${"%.2f".format(value)}, bytes=${serializedEvent.length}")
    }

    producer.flush()
    producer.close()
    logger.info("\n All 20 messages produced and sent to Kafka\n")
  }

  def consumeAndProcess(): Unit = {
    val logger = Logger.getLogger(getClass.getName)
    logger.info("\n=== STEP 3: CONSUMING AND PROCESSING MESSAGES ===\n")

    val spark = SparkSession.builder()
      .appName("UserEventProcessor")
      .master("local[*]")
      .getOrCreate()

    import spark.implicits._
    spark.sparkContext.setLogLevel("WARN")

    logger.info("Reading from Kafka topic: user-events")

    val kafkaParams = Map(
      "kafka.bootstrap.servers" -> "localhost:9092",
      "subscribe" -> "user-events",
      "startingOffsets" -> "earliest",
      "failOnDataLoss" -> "false"
    )

    val rawDF = spark.readStream
      .format("kafka")
      .options(kafkaParams)
      .load()

    logger.info("Registered Protobuf deserializer UDF")

    spark.udf.register("deserializeProtobuf", (bytes: Array[Byte]) => {
      Try({
        val message = DynamicMessage.parseFrom(descriptor, bytes)
        val userId = message.getField(descriptor.findFieldByName("userId")).asInstanceOf[Integer].toInt
        val action = message.getField(descriptor.findFieldByName("action")).asInstanceOf[String]
        val value = message.getField(descriptor.findFieldByName("value")).asInstanceOf[Double]
        (userId, action, value)
      }).toOption match {
        case Some(result) => result
        case None => (0, "unknown", 0.0)
      }
    })

    val deserializedDF = rawDF
      .filter($"value".isNotNull)
      .select(callUDF("deserializeProtobuf", $"value").as("parsed"))
      .select(
        $"parsed._1".as("userId"),
        $"parsed._2".as("action"),
        $"parsed._3".as("value")
      )

    logger.info("Deserialized 20 messages from Protobuf binary format\n")

    val eventCountDF = deserializedDF
      .filter($"action" !== "unknown")
      .groupBy($"action")
      .count()
      .withColumnRenamed("count", "eventCount")
      .orderBy(desc("eventCount"))

    val topUsersDF = deserializedDF
      .filter($"userId" !== 0)
      .groupBy($"userId")
      .agg(sum($"value").as("totalValue"))
      .orderBy(desc("totalValue"))
      .limit(5)

    logger.info("Starting streaming analysis...\n")

    val eventCountQuery = eventCountDF
      .writeStream
      .outputMode("complete")
      .format("console")
      .option("truncate", "false")
      .start()

    val topUsersQuery = topUsersDF
      .writeStream
      .outputMode("complete")
      .format("console")
      .option("truncate", "false")
      .start()

    Thread.sleep(15000)
    spark.streams.awaitAnyTermination()


    spark.stop()

    logger.info("\n Processing completed\n")
  }

  def main(args: Array[String]): Unit = {
    val logger = Logger.getLogger(getClass.getName)

    logger.info("\n" + "=" * 70)
    logger.info("EXERCISE 5: PROTOBUF WITH KAFKA - COMPLETE WORKFLOW")
    logger.info("=" * 70)

    loadDescriptorFile("src/main/resources/UserEvent.desc")

    Thread.sleep(2000)
    produceMessages()

    Thread.sleep(5000)
    consumeAndProcess()

    logger.info("=" * 70)
    logger.info("WORKFLOW COMPLETED SUCCESSFULLY")
    logger.info("=" * 70)
  }
}