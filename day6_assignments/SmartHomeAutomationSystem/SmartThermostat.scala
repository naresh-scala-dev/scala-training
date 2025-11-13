class SmartThermostat extends Device with Connectivity {
  override def turnOn(): Unit = println("SmartThermostat heating/cooling ON")
  override def turnOff(): Unit = println("SmartThermostat system OFF")
}
