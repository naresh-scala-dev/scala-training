class BasicDrone extends Drone {
  override def activate(): Unit = println("Drone activated")
  override def deactivate(): Unit = println("Drone deactivated")
}
