object LazyPipeline {

  object Pipeline {
    def apply[T](block: => T): LazyPipeline[T] =
      new LazyPipeline(block)
  }

  class LazyPipeline[T](block: => T) {
    lazy val result: T = block

    def map[R](f: T => R): LazyPipeline[R] =
      Pipeline(f(result))
  }

  def main(args: Array[String]): Unit = {
    val pipeline = Pipeline {
      println("Step 1: Preparing data")
      List(1, 2, 3)
    }.map { data =>
      println("Step 2: Transforming data")
      data.map(_ * 2)
    }

    println("Before accessing pipeline...")
    println("Result: " + pipeline.result)
  }
}
