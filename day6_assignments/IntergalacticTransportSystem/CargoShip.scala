class CargoShip(fuelLevel: Int) extends Spacecraft(fuelLevel) with Autopilot {

  override def launch(): Unit = println(
    "cargoShip is lunching with fuel level: " + fuelLevel
  )

  override def land(): Unit = println("Cargoship landing sequence is initiated")

  override def autoNavigate(): Unit = println("CargoShip autopilot activated")

}
