package util


import config.PipelineConfiguration
import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.sql.SparkSession

object SparkSessionFactory extends LazyLogging {
  @volatile private var sparkSessionInstance: Option[SparkSession] = None

  def getOrCreateSession(): SparkSession = {
    sparkSessionInstance match {
      case Some(s) if !s.sparkContext.isStopped =>
        logger.info("Reusing existing Spark session")
        s
      case _ =>
        logger.info("Creating new Spark session")
        synchronized {
          sparkSessionInstance match {
            case Some(s) if !s.sparkContext.isStopped => s
            case _ =>
              val builder = SparkSession.builder()
                .appName(PipelineConfiguration.Spark.appName)
                .master(PipelineConfiguration.Spark.master)
                .config("spark.sql.shuffle.partitions", PipelineConfiguration.Spark.shufflePartitions)
                .config("spark.sql.adaptive.enabled", PipelineConfiguration.Spark.adaptiveEnabled)
                .config("spark.sql.broadcastTimeout", PipelineConfiguration.Spark.broadcastTimeout)
                .config("spark.executor.memory", PipelineConfiguration.Spark.executorMemory)
                .config("spark.executor.cores", PipelineConfiguration.Spark.executorCores)
                .config("spark.driver.memory", PipelineConfiguration.Spark.driverMemory)
                .config("spark.cassandra.connection.host", PipelineConfiguration.Cassandra.host)
                .config("spark.cassandra.connection.port", PipelineConfiguration.Cassandra.port)
                .config("spark.cassandra.auth.username", PipelineConfiguration.Cassandra.username)
                .config("spark.cassandra.auth.password", PipelineConfiguration.Cassandra.password)
                .config("spark.cassandra.output.batch.size.rows", PipelineConfiguration.Cassandra.writeBatchSize)
                .config("spark.cassandra.output.concurrent.writes", PipelineConfiguration.Cassandra.writeParallelism)
                .config("spark.cassandra.output.consistency.level", "LOCAL_QUORUM")
              if (PipelineConfiguration.Cassandra.sslEnabled) {
                builder
                  .config("spark.cassandra.connection.ssl.enabled", "true")
                  .config("spark.cassandra.connection.ssl.trustStore.path", PipelineConfiguration.Cassandra.truststorePath)
                  .config("spark.cassandra.connection.ssl.trustStore.password", PipelineConfiguration.Cassandra.truststorePassword)
              }

              val session = builder.getOrCreate()
              logger.info(s"Spark session created - version=${session.version}, master=${session.sparkContext.master}")
              sparkSessionInstance = Some(session)
              session
          }
        }
    }
  }

  def stopSession(): Unit = {
    sparkSessionInstance.foreach { session =>
      logger.info("Stopping Spark session")
      session.stop()
      sparkSessionInstance = None
    }
  }
}
