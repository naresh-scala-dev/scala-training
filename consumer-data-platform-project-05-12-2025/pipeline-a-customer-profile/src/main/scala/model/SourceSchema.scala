package model


import org.apache.spark.sql.types._

object SourceSchema {
  val customersSchema: StructType = StructType(Array(
    StructField("customer_id", IntegerType, nullable = false),
    StructField("name", StringType, nullable = false),
    StructField("email", StringType, nullable = false),
    StructField("gender", StringType, nullable = false),
    StructField("signup_date", DateType, nullable = false)
  ))

  val productsSchema: StructType = StructType(Array(
    StructField("product_id", IntegerType, nullable = false),
    StructField("name", StringType, nullable = false),
    StructField("category", StringType, nullable = false),
    StructField("price", DecimalType(10, 2), nullable = false)
  ))

  val transactionsSchema: StructType = StructType(Array(
    StructField("txn_id", IntegerType, nullable = false),
    StructField("customer_id", IntegerType, nullable = false),
    StructField("product_id", IntegerType, nullable = false),
    StructField("qty", IntegerType, nullable = false),
    StructField("amount", DecimalType(10, 2), nullable = false),
    StructField("txn_timestamp", TimestampType, nullable = false)
  ))
}
