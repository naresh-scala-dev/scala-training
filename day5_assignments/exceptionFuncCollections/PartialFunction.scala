object PartialFunction {

  def main(args: Array[String]): Unit = {

    val items: List[Any] = List(1, "apple", 3.5, "banana", 42)

    val doubleInts: PartialFunction[Any, Int] = { case i: Int =>
      i * 2
    }

    val result = items.collect(doubleInts)

    println(result)
  }
}
