trait EnergySaver extends Device {
  def activateEnergySaver(): Unit = println("Energy saver mode activated")
  override def turnOff(): Unit = println("Device powered down to save energy")
}
