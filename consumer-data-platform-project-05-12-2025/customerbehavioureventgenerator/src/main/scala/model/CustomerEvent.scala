package model

case class CustomerEvent(
                          event_id: String,
                          customer_id: Int,
                          product_id: Option[Int],
                          event_type: String,
                          event_timestamp: Long,
                          ingestion_timestamp: Long
                        )