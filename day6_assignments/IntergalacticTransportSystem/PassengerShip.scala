class PassengerShip(fuelLevel: Int) extends Spacecraft(fuelLevel) {

  override def launch(): Unit = println(
    "cargoShip is lunching passengers with fuel level: " + fuelLevel
  )

  final override def land(): Unit = println("PassengerShip is landing smoothly")

}
