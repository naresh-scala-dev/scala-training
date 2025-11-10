object MultiplicationTable {

  def multiplicationTable(size: Int): List[String] = {
    (for {
      i <- 1 to size
      j <- 1 to size
    } yield s"$i x $j = ${i * j}").toList
  }

  // ganeral
  def multiTable(num: Int): Unit = {
    for (i <- 1 to num) {
      for (j <- 1 to num) {
        println(s"$i x $j = ${i * j}")
      }
    }
  }

  def main(args: Array[String]): Unit = {
    val table = multiplicationTable(3)
    table.foreach(println)
    println("*****************************************")
    multiTable(5)
  }
}
