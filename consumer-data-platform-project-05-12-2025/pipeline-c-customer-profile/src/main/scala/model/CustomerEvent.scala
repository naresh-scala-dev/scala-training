package model

import java.sql.Timestamp

case class CustomerEvent(
                          event_id: String,
                          customer_id: Int,
                          product_id: Option[Int],
                          event_type: String,
                          event_timestamp: Timestamp,
                          ingestion_timestamp: Timestamp,
                          event_date: String
                        )