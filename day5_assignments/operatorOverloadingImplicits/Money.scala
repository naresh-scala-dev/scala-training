object Money{

  case class Money(amount: Double) {
    def +(that: Money)(using precision: Double): Money = 
      Money(roundToPrecision(this.amount + that.amount))
    
    def -(that: Money)(using precision: Double): Money = 
      Money(roundToPrecision(this.amount - that.amount))

    private def roundToPrecision(value: Double)(using precision: Double): Double =
      (math.round(value / precision) * precision * 100).round / 100.0

    override def toString: String = f"₹$amount%.2f"
  }

  def main(args: Array[String]): Unit = {
    given roundingPrecision: Double = 0.05

    val m1 = Money(10.23)
    val m2 = Money(5.19)

    println(m1 + m2)
    println(m1 - m2)
  }
}
