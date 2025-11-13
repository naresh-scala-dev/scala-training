object TrafficMain {
  def main(args: Array[String]): Unit = {
    var continueProgram = true

    while (continueProgram) {
      println(
        """
          |========= SMART TRAFFIC MANAGEMENT SYSTEM =========
          |1. Add Vehicle
          |2. Add Traffic Signal
          |3. Record Violation
          |4. Update Signal Status
          |5. View Vehicles
          |6. View Traffic Signals
          |7. View Violations
          |8. Delete Vehicle
          |9. Delete Violation
          |10. Exit
          |===================================================
          |Enter your choice:
          |""".stripMargin
      )

      val choice = scala.io.StdIn.readInt()

      choice match {
        case 1 =>
          println("Enter license plate:")
          val licensePlate = scala.io.StdIn.readLine()
          println("Enter vehicle type:")
          val vehicleType = scala.io.StdIn.readLine()
          println("Enter owner name:")
          val ownerName = scala.io.StdIn.readLine()
          TrafficDAO.addVehicle(licensePlate, vehicleType, ownerName)

        case 2 =>
          println("Enter signal location:")
          val signalLocation = scala.io.StdIn.readLine()
          println("Enter signal status (green/yellow/red):")
          val signalStatus = scala.io.StdIn.readLine()
          TrafficDAO.addTrafficSignal(signalLocation, signalStatus)

        case 3 =>
          println("Enter vehicle ID:")
          val vehicleId = scala.io.StdIn.readInt()
          println("Enter signal ID:")
          val signalId = scala.io.StdIn.readInt()
          println("Enter violation type:")
          val violationType = scala.io.StdIn.readLine()
          TrafficDAO.recordViolation(vehicleId, signalId, violationType)

        case 4 =>
          println("Enter signal ID:")
          val signalId = scala.io.StdIn.readInt()
          println("Enter new signal status:")
          val newStatus = scala.io.StdIn.readLine()
          TrafficDAO.updateSignalStatus(signalId, newStatus)

        case 5 =>
          TrafficDAO.viewVehicles()

        case 6 =>
          TrafficDAO.viewSignals()

        case 7 =>
          TrafficDAO.viewViolations()

        case 8 =>
          println("Enter vehicle ID to delete:")
          val vehicleId = scala.io.StdIn.readInt()
          TrafficDAO.deleteVehicle(vehicleId)

        case 9 =>
          println("Enter violation ID to delete:")
          val violationId = scala.io.StdIn.readInt()
          TrafficDAO.deleteViolation(violationId)

        case 10 =>
          println("Exiting system. Goodbye.")
          continueProgram = false

        case _ =>
          println("Invalid choice. Please try again.")
      }
    }
  }
}
