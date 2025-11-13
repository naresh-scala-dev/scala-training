trait Connectivity {
  def connect(): Unit = println("Device connected to network")
  def disconnect(): Unit = println("Device disconnected")
}
