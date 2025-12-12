package models

import play.api.libs.json._
import java.sql.Timestamp

case class CustomerEvent(
                          event_id: String,
                          customer_id: Int,
                          event_type: String,
                          product_id: Option[Int],
                          event_timestamp: Timestamp,
                          ingestion_timestamp: Timestamp
                        )

object CustomerEvent {
  implicit val timestampFormat: Format[Timestamp] = new Format[Timestamp] {
    def reads(json: JsValue): JsResult[Timestamp] = json match {
      case JsString(s) => JsSuccess(Timestamp.valueOf(s))
      case _ => JsError("Timestamp expected")
    }
    def writes(ts: Timestamp): JsValue = JsString(ts.toString)
  }

  implicit val customerEventFormat: OFormat[CustomerEvent] = Json.format[CustomerEvent]
}