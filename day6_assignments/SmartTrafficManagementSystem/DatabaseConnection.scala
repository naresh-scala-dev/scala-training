package database

import java.sql.{Connection, DriverManager, SQLException}
import java.util.Properties
import java.io.FileInputStream

object DatabaseConnection {

  private val properties = new Properties()

  try {

    properties.load(new FileInputStream("config/db.properties"))
  } catch {
    case e: Exception =>
      e.printStackTrace()
      throw new RuntimeException("Failed to load database properties file")
  }

  private val url: String = properties.getProperty("db.url")
  private val username: String = properties.getProperty("db.username")
  private val password: String = properties.getProperty("db.password")

  // Establish connection
  def getConnection(): Connection = {
    try {
      Class.forName("com.mysql.cj.jdbc.Driver")
      DriverManager.getConnection(url, username, password)
    } catch {
      case e: SQLException =>
        e.printStackTrace()
        throw new RuntimeException("Database connection error")
      case e: ClassNotFoundException =>
        e.printStackTrace()
        throw new RuntimeException("MySQL JDBC Driver not found")
    }
  }
}
