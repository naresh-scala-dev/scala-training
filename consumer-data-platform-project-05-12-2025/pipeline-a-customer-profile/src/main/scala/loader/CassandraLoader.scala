package loader



import com.datastax.spark.connector.cql.CassandraConnector
import config.PipelineConfiguration
import model.CustomerProfile
import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}

class CassandraLoader(spark: SparkSession) extends LazyLogging {
  private val keyspace = PipelineConfiguration.Cassandra.keyspace
  private val table = CustomerProfile.TABLE_NAME

  def initializeSchema(): Unit = {
    logger.info(s"Initializing Cassandra keyspace=$keyspace, table=$table")
    CassandraConnector(spark.sparkContext.getConf).withSessionDo { session =>
      session.execute(
        s"""
           |CREATE KEYSPACE IF NOT EXISTS $keyspace
           |WITH replication = {'class': 'SimpleStrategy', 'replication_factor': ${PipelineConfiguration.Cassandra.replicationFactor}}
           |""".stripMargin
      )

      session.execute(
        s"""
           |CREATE TABLE IF NOT EXISTS $keyspace.$table (
           |  customer_id INT PRIMARY KEY,
           |  name TEXT,
           |  email TEXT,
           |  gender TEXT,
           |  total_spend DECIMAL,
           |  total_transactions BIGINT,
           |  avg_order_value DECIMAL,
           |  first_purchase TIMESTAMP,
           |  last_purchase TIMESTAMP,
           |  favorite_category TEXT
           |)""".stripMargin
      )
    }
    logger.info("Cassandra schema initialization complete")
  }

  def loadProfiles(profiles: DataFrame): Unit = {
    val cnt = profiles.count()
    logger.info(s"Writing $cnt profiles to Cassandra (keyspace=$keyspace, table=$table)")
    // Repartition for parallel writes
    val writeParallelism = PipelineConfiguration.Cassandra.writeParallelism
    val repartitionedProfiles = profiles.repartition(writeParallelism)

    try {
      repartitionedProfiles.write
        .format("org.apache.spark.sql.cassandra")
        .mode(SaveMode.Append)
        .option("keyspace", keyspace)
        .option("table", table)
        .option("consistency.level", "LOCAL_QUORUM")
        .save()
      logger.info("Profiles written to Cassandra successfully")
    } catch {
      case e: Exception =>
        logger.error(s"Failed to write to Cassandra: ${e.getMessage}", e)
        throw e
    }
  }
}
