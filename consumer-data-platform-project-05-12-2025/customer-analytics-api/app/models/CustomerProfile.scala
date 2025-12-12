package models

import play.api.libs.json._
import java.sql.Timestamp

case class CustomerProfile(
                            customer_id: Int,
                            name: String,
                            email: String,
                            gender: String,
                            total_spend: BigDecimal,
                            total_transactions: Int,
                            avg_order_value: BigDecimal,
                            first_purchase: Option[Timestamp],
                            last_purchase: Option[Timestamp],
                            favorite_category: String
                          )

object CustomerProfile {
  implicit val timestampFormat: Format[Timestamp] = new Format[Timestamp] {
    def reads(json: JsValue): JsResult[Timestamp] = json match {
      case JsString(s) => JsSuccess(Timestamp.valueOf(s))
      case _ => JsError("Timestamp expected")
    }
    def writes(ts: Timestamp): JsValue = JsString(ts.toString)
  }

  implicit val optionalTimestampFormat: Format[Option[Timestamp]] = Format.optionWithNull[Timestamp]
  implicit val customerProfileFormat: OFormat[CustomerProfile] = Json.format[CustomerProfile]
}