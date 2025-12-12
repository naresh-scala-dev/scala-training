  package config

  import com.typesafe.config.ConfigFactory

  /**
   * Purpose:
   * Centralized configuration loader for Akka Event Generator.
   * All runtime values are externalized via application.conf.
   */

  object AppConfig {
    private val config = ConfigFactory.load()

    object akka {
      object eventGenerator {
        val eventsPerSecond: Int = config.getInt("akka.event-generator.events-per-second")
        val startupDelaySeconds: Int = config.getInt("akka.event-generator.startup-delay-seconds")
      }
    }

    object kafka {
      val bootstrapServers: String = config.getString("kafka.bootstrap-servers")
      val topic: String = config.getString("kafka.topic")
    }

    object mysql {
      val url: String = config.getString("mysql.url")
      val user: String = config.getString("mysql.user")
      val password: String = config.getString("mysql.password")
    }
  }
