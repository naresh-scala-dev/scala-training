object AdaptiveDiscount {

  def discountStrategy(memberType: String): Double => Double =
    memberType.toLowerCase match {
      case "gold"   => price => price * 0.8
      case "silver" => price => price * 0.9
      case _        => price => price
    }

  def main(args: Array[String]): Unit = {
    val goldDiscount = discountStrategy("gold")
    val silverDiscount = discountStrategy("silver")
    val regularDiscount = discountStrategy("regular")

    println(goldDiscount(1000))
    println(silverDiscount(1000))
    println(regularDiscount(1000))
  }
}
