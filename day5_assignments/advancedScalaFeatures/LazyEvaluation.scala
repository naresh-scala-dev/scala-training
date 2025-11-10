object LazyEvaluation {

  class LazyCounter {
    private var computeCount = 0

    lazy val value: Int = {
      computeCount += 1
      println("Computing value...")
      42
    }

    def getCount: Int = computeCount
  }

  def main(args: Array[String]): Unit = {
    val counter = new LazyCounter

    println("Before first access")
    println(counter.value)
    println("Access again")
    println(counter.value)
    println("Compute count: " + counter.getCount)
  }
}
