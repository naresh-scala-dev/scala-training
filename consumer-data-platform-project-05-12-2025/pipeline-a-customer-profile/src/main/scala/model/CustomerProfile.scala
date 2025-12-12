package model

import java.sql.Timestamp

case class CustomerProfile(
                            customer_id: Int,
                            name: String,
                            email: String,
                            gender: String,
                            total_spend: BigDecimal,
                            total_transactions: Long,
                            avg_order_value: BigDecimal,
                            first_purchase: Timestamp,
                            last_purchase: Timestamp,
                            favorite_category: String
                          )

object CustomerProfile {
  val TABLE_NAME = "customer_profile"
}
