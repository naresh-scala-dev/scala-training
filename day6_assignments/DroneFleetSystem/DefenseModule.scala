trait DefenseModule extends Drone {
  def activateShields(): Unit = println("Defense shields activated")
  abstract override def deactivate(): Unit = {
    super.deactivate()
    println("Defense systems deactivated")
  }
}
