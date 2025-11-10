object Counter {

  class Counter(val value: Int) {
    def +(that: Counter): Int = this.value + that.value
    def +(that: Int): Int = this.value + that
    override def toString: String = value.toString
  }

  def main(args: Array[String]): Unit = {
    val a = new Counter(5)
    val b = new Counter(7)

    println(a + b)
    println(a + 10)
    println(b + 20)

    val c = new Counter(3)
    println(a + c)
    println(c + 50)
  }

}
