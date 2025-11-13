class SmartLight extends Device with Connectivity with EnergySaver {
  override def turnOn(): Unit = println("SmartLight is now ON")
}
