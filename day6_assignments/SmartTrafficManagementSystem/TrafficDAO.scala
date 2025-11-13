import java.sql.{Connection, PreparedStatement, ResultSet, Timestamp}
import java.time.LocalDateTime

object TrafficDAO {

  def addVehicle(
      licensePlate: String,
      vehicleType: String,
      ownerName: String
  ): Unit = {
    val connection = DatabaseConnection.getConnection()
    val query =
      "INSERT INTO Vehicles (license_plate, vehicle_type, owner_name) VALUES (?, ?, ?)"
    val statement = connection.prepareStatement(query)
    statement.setString(1, licensePlate)
    statement.setString(2, vehicleType)
    statement.setString(3, ownerName)
    statement.executeUpdate()
    println("Vehicle added successfully.")
    connection.close()
  }

  def addTrafficSignal(signalLocation: String, signalStatus: String): Unit = {
    val connection = DatabaseConnection.getConnection()
    val query = "INSERT INTO TrafficSignals (location, status) VALUES (?, ?)"
    val statement = connection.prepareStatement(query)
    statement.setString(1, signalLocation)
    statement.setString(2, signalStatus)
    statement.executeUpdate()
    println("Traffic signal added successfully.")
    connection.close()
  }

  def recordViolation(
      vehicleId: Int,
      signalId: Int,
      violationType: String
  ): Unit = {
    val connection = DatabaseConnection.getConnection()
    val query =
      "INSERT INTO Violations (vehicle_id, signal_id, violation_type, timestamp) VALUES (?, ?, ?, ?)"
    val statement = connection.prepareStatement(query)
    statement.setInt(1, vehicleId)
    statement.setInt(2, signalId)
    statement.setString(3, violationType)
    statement.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()))
    statement.executeUpdate()
    println("Violation recorded successfully.")
    connection.close()
  }

  def updateSignalStatus(signalId: Int, newStatus: String): Unit = {
    val connection = DatabaseConnection.getConnection()
    val query = "UPDATE TrafficSignals SET status = ? WHERE signal_id = ?"
    val statement = connection.prepareStatement(query)
    statement.setString(1, newStatus)
    statement.setInt(2, signalId)
    statement.executeUpdate()
    println("Signal status updated successfully.")
    connection.close()
  }

  def viewVehicles(): Unit = {
    val connection = DatabaseConnection.getConnection()
    val resultSet =
      connection.createStatement().executeQuery("SELECT * FROM Vehicles")
    println("Vehicles List:")
    while (resultSet.next()) {
      println(
        s"ID: ${resultSet.getInt("vehicle_id")}, " +
          s"Plate: ${resultSet.getString("license_plate")}, " +
          s"Type: ${resultSet.getString("vehicle_type")}, " +
          s"Owner: ${resultSet.getString("owner_name")}"
      )
    }
    connection.close()
  }

  def viewSignals(): Unit = {
    val connection = DatabaseConnection.getConnection()
    val resultSet =
      connection.createStatement().executeQuery("SELECT * FROM TrafficSignals")
    println("Traffic Signals List:")
    while (resultSet.next()) {
      println(
        s"ID: ${resultSet.getInt("signal_id")}, " +
          s"Location: ${resultSet.getString("location")}, " +
          s"Status: ${resultSet.getString("status")}"
      )
    }
    connection.close()
  }

  def viewViolations(): Unit = {
    val connection = DatabaseConnection.getConnection()
    val resultSet =
      connection.createStatement().executeQuery("SELECT * FROM Violations")
    println("Violations List:")
    while (resultSet.next()) {
      println(
        s"ID: ${resultSet.getInt("violation_id")}, " +
          s"Vehicle: ${resultSet.getInt("vehicle_id")}, " +
          s"Signal: ${resultSet.getInt("signal_id")}, " +
          s"Type: ${resultSet.getString("violation_type")}, " +
          s"Time: ${resultSet.getTimestamp("timestamp")}"
      )
    }
    connection.close()
  }

  def deleteVehicle(vehicleId: Int): Unit = {
    val connection = DatabaseConnection.getConnection()
    val statement =
      connection.prepareStatement("DELETE FROM Vehicles WHERE vehicle_id = ?")
    statement.setInt(1, vehicleId)
    statement.executeUpdate()
    println("Vehicle deleted successfully.")
    connection.close()
  }

  def deleteViolation(violationId: Int): Unit = {
    val connection = DatabaseConnection.getConnection()
    val statement = connection.prepareStatement(
      "DELETE FROM Violations WHERE violation_id = ?"
    )
    statement.setInt(1, violationId)
    statement.executeUpdate()
    println("Violation deleted successfully.")
    connection.close()
  }
}
