package models

import play.api.libs.json._
import java.sql.Date

case class AggregatedDailySummary(
                                   date: Date,
                                   totalCustomers: Int,
                                   totalAmount: BigDecimal,
                                   totalItems: Long,
                                   avgOrderValue: BigDecimal,
                                   topCategory: Option[String],
                                   summaries: List[DailySummary]
                                 )

object AggregatedDailySummary {
  implicit val dateFormat: Format[Date] = new Format[Date] {
    def reads(json: JsValue): JsResult[Date] = json match {
      case JsString(s) => JsSuccess(Date.valueOf(s))
      case _ => JsError("Date expected")
    }
    def writes(date: Date): JsValue = JsString(date.toString)
  }

  implicit val aggregatedDailySummaryFormat: OFormat[AggregatedDailySummary] = Json.format[AggregatedDailySummary]

  def fromList(list: List[DailySummary]): AggregatedDailySummary = {
    require(list.nonEmpty, "DailySummary list cannot be empty")

    val date = list.head.date
    val totalCustomers = list.size
    val totalAmount = list.map(_.total_amount).foldLeft(BigDecimal(0))(_ + _)
    val totalItems = list.map(_.total_items).sum
    val avgOrderValue = if (totalCustomers == 0) BigDecimal(0) else totalAmount / totalCustomers
    val topCategory = list.flatMap(_.top_category).groupBy(identity).mapValues(_.size).maxByOption(_._2).map(_._1)

    AggregatedDailySummary(date, totalCustomers, totalAmount, totalItems, avgOrderValue, topCategory, list)
  }
}
