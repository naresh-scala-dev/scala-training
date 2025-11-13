object SmartHomeMain {

  def main(args: Array[String]): Unit = {
    val light = new SmartLight
    val thermostat = new SmartThermostat

    light.turnOn()
    light.turnOff()
    light.status()
    light.connect()
    light.activateEnergySaver()

    println("---")

    thermostat.turnOn()
    thermostat.turnOff()
    thermostat.status()
    thermostat.connect()

  }
}
