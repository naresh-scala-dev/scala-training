object PersonalizedCalculator {

  def calculate(operation: String)(a: Int, b: Int): Int = operation match {
    case "add" => a + b
    case "sub" => a - b
    case "mul" => a * b
    case "div" => a / b
    case _     => throw new IllegalArgumentException("Unsupported operation")
  }

  def main(args: Array[String]): Unit = {
    val adder = calculate("add")
    val subtractor = calculate("sub")
    val multiplier = calculate("mul")
    val divider = calculate("div")

    println(adder(10, 5))
    println(subtractor(10, 5))
    println(multiplier(3, 4))
    println(divider(20, 4))
  }
}
