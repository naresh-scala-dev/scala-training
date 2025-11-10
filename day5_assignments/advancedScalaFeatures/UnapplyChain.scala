object UnapplyChain {

  case class Address(city: String, pincode: Int)
  case class Person(name: String, address: Address)

  def main(args: Array[String]): Unit = {
    val person = Person("Ravi", Address("Chennai", 600001))

    person match
      case Person(_, Address(city, pin)) if city.startsWith("C") =>
        println(s"$city - $pin")
      case _ =>
        println("No match")
  }
}
