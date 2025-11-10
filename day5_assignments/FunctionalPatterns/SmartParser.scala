object SmartParser {

  def safeDivide(x: Int, y: Int): Option[Int] =
    if y == 0 then None else Some(x / y)

  def parseAndDivide(input: String): Either[String, Int] =
    input.toIntOption match
      case None => Left("Invalid number")
      case Some(number) =>
        safeDivide(100, number) match
          case None        => Left("Division by zero")
          case Some(result) => Right(result)

  def main(args: Array[String]): Unit = {
    println(parseAndDivide("25"))
    println(parseAndDivide("0"))
    println(parseAndDivide("abc"))
  }
}
