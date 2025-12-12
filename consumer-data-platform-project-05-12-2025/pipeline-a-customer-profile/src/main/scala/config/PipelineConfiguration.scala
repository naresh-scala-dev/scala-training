package config


import com.typesafe.config.{Config, ConfigFactory}


object PipelineConfiguration {
  private val config: Config = ConfigFactory.load()

  object Pipeline {
    val name: String = config.getString("pipeline.name")
    val version: String = config.getString("pipeline.version")
    val mode: String = config.getString("pipeline.mode")
    val frequency: String = config.getString("pipeline.frequency")
  }

  object MySQL {
    val host: String = config.getString("mysql.host")
    val port: Int = config.getInt("mysql.port")
    val database: String = config.getString("mysql.database")
    val username: String = config.getString("mysql.username")
    val password: String = config.getString("mysql.password")
    val driver: String = config.getString("mysql.driver")

    val fetchSize: Int = config.getInt("mysql.fetch-size")
    val partitionColumn: String = config.getString("mysql.partition-column")
    val numPartitions: Int = config.getInt("mysql.num-partitions")
    val lowerBound: Int = config.getInt("mysql.lower-bound")
    val upperBound: Int = config.getInt("mysql.upper-bound")

    def jdbcUrl: String = s"jdbc:mysql://$host:$port/$database"

    def connectionProperties: Map[String, String] = Map(
      "user" -> username,
      "password" -> password,
      "driver" -> driver,
      "useSSL" -> config.getString("mysql.connection-properties.useSSL"),
      "allowPublicKeyRetrieval" -> config.getString("mysql.connection-properties.allowPublicKeyRetrieval"),
      "serverTimezone" -> config.getString("mysql.connection-properties.serverTimezone"),
      "rewriteBatchedStatements" -> config.getString("mysql.connection-properties.rewriteBatchedStatements")
    )
  }

  object Cassandra {
    val host: String = config.getString("cassandra.host")
    val port: Int = config.getInt("cassandra.port")
    val keyspace: String = config.getString("cassandra.keyspace")
    val username: String = config.getString("cassandra.username")
    val password: String = config.getString("cassandra.password")

    val sslEnabled: Boolean = config.getBoolean("cassandra.ssl.enabled")
    val truststorePath: String = config.getString("cassandra.ssl.truststore-path")
    val truststorePassword: String = config.getString("cassandra.ssl.truststore-password")

    val replicationFactor: Int = config.getInt("cassandra.replication-factor")
    val writeBatchSize: Int = config.getInt("cassandra.write-batch-size")
    val writeParallelism: Int = config.getInt("cassandra.write-parallelism")
  }

  object Spark {
    val appName: String = config.getString("spark.app-name")
    val master: String = config.getString("spark.master")
    val shufflePartitions: Int = config.getInt("spark.sql.shuffle-partitions")
    val adaptiveEnabled: Boolean = config.getBoolean("spark.sql.adaptive-enabled")
    val broadcastTimeout: Int = config.getInt("spark.sql.broadcast-timeout")
    val executorMemory: String = config.getString("spark.executor.memory")
    val executorCores: Int = config.getInt("spark.executor.cores")
    val driverMemory: String = config.getString("spark.driver.memory")
  }

  object DataQuality {
    val logInvalidRecords: Boolean = config.getBoolean("data-quality.log-invalid-records")
    val maxInvalidRecords: Int = config.getInt("data-quality.max-invalid-records")
    val failOnQualityBreach: Boolean = config.getBoolean("data-quality.fail-on-quality-breach")
  }

  object Extraction {
    val transactionLookbackYears: Int = config.getInt("extraction.transaction-lookback-years")
    val enableIncremental: Boolean = config.getBoolean("extraction.enable-incremental")
  }

  object Logging {
    val level: String = config.getString("logging.level")
    val enableMetrics: Boolean = config.getBoolean("logging.enable-metrics")
  }
}

