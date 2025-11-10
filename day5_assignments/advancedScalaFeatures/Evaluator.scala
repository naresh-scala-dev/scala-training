object Evaluator{

  object Evaluator {
    def apply(block: => Any): Unit =
      val result = block
      println("Evaluating block...")
      println(s"Result = $result")
  }

  def main(args: Array[String]): Unit = {
    Evaluator {
      val x = 5
      val y = 3
      x * y + 2
    }

    Evaluator {
      val a = 10
      val b = 20
      a + b - 5
    }
  }
}
