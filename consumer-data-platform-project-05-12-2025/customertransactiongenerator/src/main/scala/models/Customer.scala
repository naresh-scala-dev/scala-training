package models

import java.time.LocalDate

case class Customer(
                     customer_id: Int,
                     name: String,
                     email: String,
                     gender: String,
                     signup_date: LocalDate
                   )

