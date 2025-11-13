object DroneFleetMain {

  def main(args: Array[String]): Unit = {

    val navDrone = new BasicDrone with NavigationModule
    navDrone.activate()
    navDrone.flyTo("Mars Base")
    navDrone.deactivate()
    navDrone.status()

    println("---")

    val defenseDrone = new BasicDrone with DefenseModule
    defenseDrone.activate()
    defenseDrone.activateShields()
    defenseDrone.deactivate()
    defenseDrone.status()

    println("---")

    val commDrone = new BasicDrone with CommunicationModule
    commDrone.activate()
    commDrone.sendMessage("Mission complete")
    commDrone.deactivate()
    commDrone.status()

    println("---")

    val multiDrone1 = new BasicDrone with NavigationModule with DefenseModule
    multiDrone1.activate()
    multiDrone1.flyTo("Jupiter Station")
    multiDrone1.activateShields()
    multiDrone1.deactivate()
    multiDrone1.status()

    println("---")

    val multiDrone2 = new BasicDrone with DefenseModule with CommunicationModule
    multiDrone2.activate()
    multiDrone2.activateShields()
    multiDrone2.sendMessage("All systems go")
    multiDrone2.deactivate()
    multiDrone2.status()

    println("---")

    val multiDrone3 = new BasicDrone
      with NavigationModule
      with DefenseModule
      with CommunicationModule
    multiDrone3.activate()
    multiDrone3.flyTo("Saturn Orbit")
    multiDrone3.activateShields()
    multiDrone3.sendMessage("Scanning complete")
    multiDrone3.deactivate()
    multiDrone3.status()
  }
}
