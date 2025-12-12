package utils

object Timer {
  def time[R](block: => R): R = {
    val start = System.currentTimeMillis()
    val result = block
    val end = System.currentTimeMillis()
    println(s"Execution Time: ${end - start} ms")
    result
  }
}
