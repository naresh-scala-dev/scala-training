object SpaceMain {

  def main(args: Array[String]): Unit = {
    val cargo = new CargoShip(100)
    val passenger = new PassengerShip(80)
    val luxury = new LuxuryCruiser(120)

    cargo.launch()
    cargo.land()
    cargo.autoNavigate()

    passenger.launch()
    passenger.land()

    luxury.launch()
    luxury.land()
    luxury.playEntertainment()
  }
}
