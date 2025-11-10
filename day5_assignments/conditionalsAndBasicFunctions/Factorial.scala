object Factorial {

  def factorial(n: Int): Int = {
    def fact(x: Int, y: Int): Int = {
      if (y <= 1) x
      else fact(x * y, y - 1)
    }
    fact(1, n)
  }

  // general
  def fact(n: Int): Int = {
    var fact = 1
    for (i <- 1 to n) {
      fact *= i

    }
    fact
  }

  def main(args: Array[String]): Unit = {
    val number = 5
    println(s"Factorial of $number is ${factorial(number)}")
    val n = 8;
    println(s"Factorial iof $n is ${fact(n)}")
  }
}
