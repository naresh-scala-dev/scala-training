package config


import com.typesafe.config.ConfigFactory

object AppConfig {

  private val config = ConfigFactory.load()

  object cassandra {
    val host: String               = config.getString("cassandra.host")
    val port: Int                  = config.getInt("cassandra.port")

    val username: String           = config.getString("cassandra.username")
    val password: String           = config.getString("cassandra.password")

    val keyspace: String           = config.getString("cassandra.keyspace")
    val table: String              = config.getString("cassandra.table")

    val truststorePath: String     = config.getString("cassandra.sslTrustStorePath")
    val truststorePassword: String = config.getString("cassandra.sslTrustStorePassword")
  }

  object mysql {
    val url: String      = config.getString("mysql.url")
    val user: String     = config.getString("mysql.username")
    val password: String = config.getString("mysql.password")
    val table: String = config.getString("mysql.table")
  }

  object s3 {
    val endpoint: String           = config.getString("s3.endpoint")
    val region: String             = config.getString("s3.region")
    val pathStyleAccess: Boolean   = config.getBoolean("s3.pathStyleAccess")
    val sslEnabled: Boolean        = config.getBoolean("s3.sslEnabled")
    val impl: String               = config.getString("s3.impl")
    val credentialsProvider: String = config.getString("s3.credentialsProvider")
    val parquetOutputPath: String  = config.getString("s3.parquetOutputPath")
    val jsonOutputPath: String     = config.getString("s3.jsonOutputPath")
    val accessKey: String          = config.getString("s3.accessKey")
    val secretKey: String          = config.getString("s3.secretKey")

    val jsonCheckpointPath: String     = config.getString("s3.jsonCheckpointPath")
    val jsonStreamOutputPath: String     = config.getString("s3.jsonStreamOutputPath")

  }

  object kafka {
    val bootstrapServers: String = config.getString("kafka.bootstrapServers")
    val topic: String            = config.getString("kafka.topic")
  }

}
