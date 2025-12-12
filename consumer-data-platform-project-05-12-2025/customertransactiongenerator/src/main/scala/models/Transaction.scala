package models


import java.sql.Timestamp
import java.time.LocalDateTime

case class Transaction(
                        txn_id: Int,
                        customer_id: Int,
                        product_id: Int,
                        qty: Int,
                        amount: BigDecimal,
                        txn_timestamp: Timestamp
                      )

