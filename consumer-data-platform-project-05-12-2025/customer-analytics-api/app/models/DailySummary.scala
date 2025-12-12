package models

import play.api.libs.json._
import java.sql.Date

case class DailySummary(
                         date: Date,
                         customer_id: Int,
                         total_amount: BigDecimal,
                         total_items: Long,
                         distinct_products: Long,
                         top_category: Option[String]
                       )

object DailySummary {
  implicit val dateFormat: Format[Date] = new Format[Date] {
    def reads(json: JsValue): JsResult[Date] = json match {
      case JsString(s) => JsSuccess(Date.valueOf(s))
      case _ => JsError("Date expected")
    }
    def writes(date: Date): JsValue = JsString(date.toString)
  }

  implicit val dailySummaryFormat: OFormat[DailySummary] = Json.format[DailySummary]
}