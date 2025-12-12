package config

case class AppConfig(
                      mysql: MySQLConfig,
                      generation: DataGenerationConfig,
                      validation: ValidationConfig,
                      performance: PerformanceConfig
                    )

case class MySQLConfig(
                        host: String,
                        port: Int,
                        database: String,
                        username: String,
                        password: String
                      ) {
  def jdbcUrl: String =
    s"jdbc:mysql://$host:$port/$database?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&rewriteBatchedStatements=true&allowMultiQueries=true&connectionTimeout=30000"
}

case class DataGenerationConfig(
                                 customerCount: Int,
                                 productCount: Int,
                                 transactionCount: Int,
                                 qtyMin: Int,
                                 qtyMax: Int,
                                 priceMin: BigDecimal,
                                 priceMax: BigDecimal,
                                 startDate: String,
                                 endDate: String,
                                 batchSize: Int
                               )

case class ValidationConfig(
                             enabled: Boolean
                           )

case class PerformanceConfig(
                              bulkBatchSize: Int,
                              partitions: Int,
                              shufflePartitions: Int,
                              executorCores: Int,
                              executorMemory: String,
                              driverMemory: String,
                              mysqlBatchInsertSize: Int,
                              mysqlFetchSize: Int,
                              mysqlConnectionTimeout: Int
                            )