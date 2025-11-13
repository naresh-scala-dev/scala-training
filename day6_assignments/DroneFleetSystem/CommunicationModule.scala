trait CommunicationModule extends Drone {
  def sendMessage(msg: String): Unit = println(s"Transmitting message: $msg")
  abstract override def deactivate(): Unit = {
    super.deactivate()
    println("Communication module shutting down")
  }
}
