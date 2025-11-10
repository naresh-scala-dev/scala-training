object ListOperators {

  def main(args: Array[String]): Unit = {

    val nums = List(2, 4, 6)

    val addEnd = nums :+ 8
    println(addEnd)

    val addStart = 0 +: nums
    println(addStart)

    val combined = 0 +: nums :+ 8
    println(combined)

    val multipleEnd = nums :+ 7 :+ 9
    println(multipleEnd)

    val multipleStart = -1 +: -2 +: nums
    println(multipleStart)

    val fullCombined = -1 +: nums :+ 8 :+ 10
    println(fullCombined)
  }

}
