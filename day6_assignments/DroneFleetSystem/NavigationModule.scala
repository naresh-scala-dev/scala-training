trait NavigationModule extends Drone {
  def flyTo(destination: String): Unit = println(s"Flying to $destination")
  abstract override def deactivate(): Unit = {
    super.deactivate()
    println("Navigation systems shutting down")
  }
}
