// utils/JsonTimestamp.scala
package security

import play.api.libs.json._

import java.sql.Timestamp

object JsonTimestamp {
  implicit val timestampFormat: Format[Timestamp] = new Format[Timestamp] {
    def writes(ts: Timestamp): JsValue = JsString(ts.toString)

    def reads(json: JsValue): JsResult[Timestamp] = json match {
      case JsString(s) =>
        try JsSuccess(Timestamp.valueOf(s)) catch {
          case _: Exception => JsError("Invalid timestamp")
        }
      case _ => JsError("String expected")
    }
  }
}
