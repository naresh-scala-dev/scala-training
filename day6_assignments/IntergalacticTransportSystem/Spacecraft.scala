abstract class Spacecraft(val fuelLevel: Int) {

  def launch(): Unit

  def land(): Unit = println("Spacecraft is landing safely.")

}
