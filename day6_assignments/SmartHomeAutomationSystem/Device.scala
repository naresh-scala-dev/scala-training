trait Device {

  def turnOn(): Unit
  def turnOff(): Unit
  def status(): Unit = println("Device is operational")
}
