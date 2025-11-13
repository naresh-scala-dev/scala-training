object RobotSystemMain {
  def main(args: Array[String]): Unit = {

    val myRobot = new BasicRobot with SpeechModule with MovementModule
    myRobot.start()
    myRobot.status()
    myRobot.speak("Hello, human!")
    myRobot.moveForward()
    myRobot.shutdown()

    println("---")

    val energyRobot = new BasicRobot with EnergySaver with MovementModule {
      override def shutdown(): Unit = super[EnergySaver].shutdown()
    }
    energyRobot.start()
    energyRobot.status()
    energyRobot.moveBackward()
    energyRobot.activateEnergySaver()
    energyRobot.shutdown()

    println("---")

    val swappedOrderRobot = new BasicRobot
      with MovementModule
      with EnergySaver {
      override def shutdown(): Unit = super[EnergySaver].shutdown()
    }
    swappedOrderRobot.start()
    swappedOrderRobot.status()
    swappedOrderRobot.moveForward()
    swappedOrderRobot.activateEnergySaver()
    swappedOrderRobot.shutdown()
  }
}
