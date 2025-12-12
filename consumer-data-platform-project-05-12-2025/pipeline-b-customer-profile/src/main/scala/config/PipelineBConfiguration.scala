package config

import com.typesafe.config.ConfigFactory

object PipelineBConfiguration {

  private val conf = ConfigFactory.load()

  object mysql {
    val url = conf.getString("pipeline-b.mysql.url")
    val user = conf.getString("pipeline-b.mysql.username")
    val password = conf.getString("pipeline-b.mysql.password")
    val query = conf.getString("pipeline-b.mysql.query")
    val fetchSize = conf.getInt("pipeline-b.mysql.fetch-size")
    val numPartitions = conf.getInt("pipeline-b.mysql.num-partitions")
    val lowerBound = conf.getInt("pipeline-b.mysql.lower-bound")
    val upperBound = conf.getInt("pipeline-b.mysql.upper-bound")
  }

  object parquet {
    val basePath = conf.getString("pipeline-b.parquet.base-path")
    val writeParallelism = conf.getInt("pipeline-b.parquet.write-parallelism")
    val s3Endpoint = conf.getString("pipeline-b.parquet.s3.endpoint")
    val s3Region = conf.getString("pipeline-b.parquet.s3.region")
    val s3AccessKey = conf.getString("pipeline-b.parquet.s3.accessKey")
    val s3SecretKey = conf.getString("pipeline-b.parquet.s3.secretKey")
    val s3Impl = conf.getString("pipeline-b.parquet.s3.impl")
    val s3CredentialsProvider = conf.getString("pipeline-b.parquet.s3.credentials-provider")
    val s3PathStyleAccess = conf.getBoolean("pipeline-b.parquet.s3.path-style-access")
  }

  object spark {
    val appName = conf.getString("spark.app-name")
    val master = conf.getString("spark.master")
    val shufflePartitions = conf.getInt("spark.sql.shuffle-partitions")
    val adaptiveEnabled = conf.getBoolean("spark.sql.adaptive-enabled")
  }
}
