package config

import com.typesafe.config.ConfigFactory

object AppConfigC {

  private val config = ConfigFactory.load()

  object kafka {
    val bootstrapServers: String = config.getString("kafka.bootstrap-servers")
    val topic: String = config.getString("kafka.topic")
  }

  object spark {
    val appName: String = config.getString("spark.app-name")
    val master: String = config.getString("spark.master")
    val checkpointLocation: String = config.getString("spark.checkpoint-location")
    val shufflePartitions: Int = config.getInt("spark.shuffle-partitions")
    val writerParallelism: Int = config.getInt("spark.writer-parallelism")
  }

  object parquet {
    val basePath: String = config.getString("parquet.base-path")
    val writeParallelism: Int = config.getInt("parquet.write-parallelism")

    object s3 {
      val endpoint: String = config.getString("parquet.s3.endpoint")
      val region: String = config.getString("parquet.s3.region")
      val accessKey: String = config.getString("parquet.s3.accessKey")
      val secretKey: String = config.getString("parquet.s3.secretKey")
      val impl: String = config.getString("parquet.s3.impl")
      val credentialsProvider: String = config.getString("parquet.s3.credentials-provider")
      val pathStyleAccess: Boolean = config.getBoolean("parquet.s3.path-style-access")
    }
  }
}
