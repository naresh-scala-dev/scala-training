trait Drone {
  def activate(): Unit
  def deactivate(): Unit
  def status(): Unit = println("Drone is operational")
}
