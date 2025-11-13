trait Robot {
  def start(): Unit
  def shutdown(): Unit
  def status(): Unit = println("Robot is operational")
}
