package repository

import com.typesafe.scalalogging.LazyLogging
import config.AppConfig
import models.{CustomerEvent, DailySummary, AggregatedDailySummary}
import org.apache.avro.generic.GenericRecord
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.parquet.avro.AvroParquetReader
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.collection.mutable.ArrayBuffer
import java.net.URI
import java.sql.{Date, Timestamp}
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Singleton
class ParquetRepository @Inject()(implicit ec: ExecutionContext) extends LazyLogging {

  // ===== Lazy S3/Hadoop configuration =====
  private lazy val hadoopConf: Configuration = {
    val c = new Configuration()
    c.set("fs.s3a.impl", AppConfig.parquet.s3.impl)
    c.set("fs.s3a.access.key", AppConfig.parquet.s3.accessKey)
    c.set("fs.s3a.secret.key", AppConfig.parquet.s3.secretKey)
    c.set("fs.s3a.endpoint", AppConfig.parquet.s3.endpoint)
    c.setBoolean("fs.s3a.path.style.access", AppConfig.parquet.s3.pathStyleAccess)
    c.set("fs.s3a.connection.maximum", "50")
    c.set("fs.s3a.threads.max", "8")
    c.setBoolean("fs.s3a.fast.upload", true)

    // Fix for INT96 deprecated timestamp format
    c.setBoolean("parquet.avro.readInt96AsFixed", true)
    c.set("parquet.avro.add-list-element-records", "false")
    c.set("parquet.avro.write-old-list-structure", "false")
    c
  }

  // Force S3 configuration initialization (call once after login)
  def initS3Connection(): Unit = hadoopConf

  logger.info("Parquet Repository initialized successfully")

  // ===== Public API =====
  def getEventsByDate(customerId: Int, date: String): Future[Option[List[CustomerEvent]]] = Future {
    val files = listFiles(AppConfig.parquet.eventsBasePath, date, isEvent = true)
    if (files.isEmpty) None
    else {
      val events = files.flatMap(f => readParquetFile[CustomerEvent](f, record => {
        val cid = safeInt(record.get("customer_id"))
        if (cid == customerId) Some(CustomerEvent(
          event_id = safeString(record.get("event_id")).getOrElse(""),
          customer_id = cid,
          event_type = safeString(record.get("event_type")).getOrElse(""),
          product_id = safeString(record.get("product_id")).map(_.toInt),
          event_timestamp = safeTimestamp(record.get("event_timestamp")),
          ingestion_timestamp = safeTimestamp(record.get("ingestion_timestamp"))
        ))
        else None
      }))
      Some(events.toList)
    }
  }

  private def safeTimestamp(v: Any): Timestamp = {
    try {
      v match {
        case l: Long => new Timestamp(l)
        case fixed: org.apache.avro.generic.GenericData.Fixed =>
          val ba = fixed.bytes()
          if (ba.length == 12) convertInt96ToTimestamp(ba) else new Timestamp(0L)
        case ba: Array[Byte] if ba.length == 12 => convertInt96ToTimestamp(ba)
        case ts: Timestamp => ts
        case s: String => Timestamp.valueOf(s)
        case _ => new Timestamp(0L)
      }
    } catch {
      case _: Throwable => new Timestamp(0L)
    }
  }

  private def convertInt96ToTimestamp(ba: Array[Byte]): Timestamp = {
    val nanos = ByteBuffer.wrap(ba.take(8)).order(ByteOrder.LITTLE_ENDIAN).getLong
    val julianDay = ByteBuffer.wrap(ba.drop(8)).order(ByteOrder.LITTLE_ENDIAN).getInt
    val unixDays = julianDay - 2440588L
    new Timestamp(unixDays * 86400000L + nanos / 1000000L)
  }

  def getAllEventsByDate(date: String): Future[Option[List[CustomerEvent]]] = Future {
    val files = listFiles(AppConfig.parquet.eventsBasePath, date, isEvent = true)
    if (files.isEmpty) None
    else {
      val events = files.flatMap(f => readParquetFile[CustomerEvent](f, record => Some(CustomerEvent(
        event_id = safeString(record.get("event_id")).getOrElse(""),
        customer_id = safeInt(record.get("customer_id")),
        event_type = safeString(record.get("event_type")).getOrElse(""),
        product_id = safeString(record.get("product_id")).map(_.toInt),
        event_timestamp = safeTimestamp(record.get("event_timestamp")),
        ingestion_timestamp = safeTimestamp(record.get("ingestion_timestamp"))
      ))))
      Some(events.toList)
    }
  }

  def getRecentEventsByCustomer(customerId: Int, limit: Int): Future[Option[List[CustomerEvent]]] = Future {
    import java.time.LocalDate
    import java.time.format.DateTimeFormatter

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val today = LocalDate.now()
    val dates = (0 until 30).map(i => today.minusDays(i).format(formatter))

    val allEvents = dates.flatMap { date =>
      val files = listFiles(AppConfig.parquet.eventsBasePath, date, isEvent = true)
      files.flatMap(f => readParquetFile[CustomerEvent](f, record => {
        val cid = safeInt(record.get("customer_id"))
        if (cid == customerId) Some(CustomerEvent(
          event_id = safeString(record.get("event_id")).getOrElse(""),
          customer_id = cid,
          event_type = safeString(record.get("event_type")).getOrElse(""),
          product_id = safeString(record.get("product_id")).map(_.toInt),
          event_timestamp = safeTimestamp(record.get("event_timestamp")),
          ingestion_timestamp = safeTimestamp(record.get("ingestion_timestamp"))
        ))
        else None
      }))
    }.toList

    if (allEvents.isEmpty) None
    else Some(allEvents.sortBy(_.event_timestamp.getTime)(Ordering[Long].reverse).take(limit))
  }

  def getDailySummary(date: String, customerId: Int): Future[Option[DailySummary]] = Future {
    val files = listFiles(AppConfig.parquet.txnSummaryBasePath, date, isEvent = false)
    if (files.isEmpty) None
    else {
      files.view.flatMap(f => readParquetFile[DailySummary](f, record => {
        val cid = safeInt(record.get("customer_id"))
        if (cid == customerId) Some(DailySummary(
          date = Date.valueOf(date),
          customer_id = cid,
          total_amount = safeBigDecimal(record.get("total_amount")),
          total_items = safeLong(record.get("total_items")),
          distinct_products = safeLong(record.get("distinct_products")),
          top_category = safeString(record.get("top_category"))
        ))
        else None
      })).headOption
    }
  }

  def getAllDailySummaries(date: String): Future[Option[AggregatedDailySummary]] = Future {
    val files = listFiles(AppConfig.parquet.txnSummaryBasePath, date, isEvent = false)

    if (files.isEmpty) None
    else {
      // Convert Seq to List using `.toList`
      val summaries: List[DailySummary] =
        files.flatMap(f =>
          readParquetFile[DailySummary](f, record =>
            Some(DailySummary(
              date = Date.valueOf(date),
              customer_id = safeInt(record.get("customer_id")),
              total_amount = safeBigDecimal(record.get("total_amount")),
              total_items = safeLong(record.get("total_items")),
              distinct_products = safeLong(record.get("distinct_products")),
              top_category = safeString(record.get("top_category"))
            ))
          )
        ).toList // <-- FIX HERE (Seq → List)

      if (summaries.isEmpty) None
      else Some(AggregatedDailySummary.fromList(summaries))
    }
  }

  // ===== Private helpers =====
  private def safeInt(v: Any): Int = try {
    Option(v).map(_.toString.toInt).getOrElse(0)
  } catch {
    case _: Throwable => 0
  }

  private def safeString(v: Any): Option[String] = Option(v).map(_.toString).filter(_.nonEmpty)

  private def listFiles(basePath: String, date: String, isEvent: Boolean): Seq[Path] = {
    val partitionKey = if (isEvent) "event_date" else "date"
    val fullPath = s"$basePath/$partitionKey=$date/"
    try {
      val fs = FileSystem.get(new URI(fullPath), hadoopConf)
      if (!fs.exists(new Path(fullPath))) Seq.empty
      else fs.listStatus(new Path(fullPath)).filter(_.getPath.getName.endsWith(".parquet")).map(_.getPath).toSeq
    } catch {
      case ex: Exception => logger.error(s"Error listing files: $fullPath", ex); Seq.empty
    }
  }

  private def readParquetFile[T](filePath: Path, fn: GenericRecord => Option[T]): Seq[T] = {
    val reader = AvroParquetReader.builder[GenericRecord](filePath).withConf(hadoopConf).build()
    try {
      val results = ArrayBuffer[T]()
      var record = reader.read()
      while (record != null) {
        fn(record).foreach(results += _);
        record = reader.read()
      }
      results.toSeq
    } finally {
      reader.close()
    }
  }

  private def safeLong(v: Any): Long = try {
    Option(v).map(_.toString.toLong).getOrElse(0L)
  } catch {
    case _: Throwable => 0L
  }

  private def safeBigDecimal(v: Any): BigDecimal = try {
    BigDecimal(Option(v).map(_.toString).getOrElse("0"))
  } catch {
    case _: Throwable => BigDecimal(0)
  }

}
