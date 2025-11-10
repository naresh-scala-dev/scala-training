case class Rational(num: Int, den: Int) {
  require(den != 0, "Denominator cannot be zero")

  def /(other: Rational): Rational =
    Rational(this.num * other.den, this.den * other.num)

  override def toString: String = s"Rational($num,$den)"
}

object Rational {
  implicit def intToRational(value: Int): Rational = Rational(value, 1)

  def main(args: Array[String]): Unit = {
    val twoThirds = Rational(2, 3)
    val oneDivTwoThirds = 1 / twoThirds
    val threeDivFourFifths = 3 / Rational(4, 5)

    println(oneDivTwoThirds)
    println(threeDivFourFifths)

    val regularDivision = 10 / 2
    println(regularDivision)
  }
}
