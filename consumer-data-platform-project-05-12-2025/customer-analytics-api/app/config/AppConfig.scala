package config

import com.typesafe.config.ConfigFactory

object AppConfig {
  private val conf = ConfigFactory.load()

  object cassandra {
    val host: String = conf.getString("cassandra.host")
    val port: Int = conf.getInt("cassandra.port")
    val datacenter: String = conf.getString("cassandra.datacenter")
    val keyspace: String = conf.getString("cassandra.keyspace")
    val username: String = conf.getString("cassandra.username")
    val password: String = conf.getString("cassandra.password")
    val sslEnabled: Boolean = conf.getBoolean("cassandra.ssl.enabled")
    val truststorePath: String = conf.getString("cassandra.truststore.path")
    val truststorePassword: String = conf.getString("cassandra.truststore.password")
    val requestTimeout: Int = conf.getInt("cassandra.request-timeout")
    val connectionPoolSize: Int = conf.getInt("cassandra.connection-pool-size")
  }

  object parquet {
    val txnSummaryBasePath: String = conf.getString("parquet.txn-summary-base-path")
    val eventsBasePath: String = conf.getString("parquet.events-base-path")

    object s3 {
      val endpoint: String = conf.getString("parquet.s3.endpoint")
      val region: String = conf.getString("parquet.s3.region")
      val accessKey: String = conf.getString("parquet.s3.accessKey")
      val secretKey: String = conf.getString("parquet.s3.secretKey")
      val impl: String = conf.getString("parquet.s3.impl")
      val pathStyleAccess: Boolean = conf.getBoolean("parquet.s3.path-style-access")
    }
  }

  object cache {
    val ttlSeconds: Int = conf.getInt("cache.ttl-seconds")
    val maxSize: Int = conf.getInt("cache.max-size")
  }
}