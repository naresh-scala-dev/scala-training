import scala.annotation.tailrec

object MaxElementInArray {

  def maxInArray(arr: Array[Int]): Int = {

    @tailrec // it ensures tail recursion
    def maxArr(i: Int, maxVal: Int): Int = {
      if (i == arr.length) maxVal
      else maxArr(i + 1, if (arr(i) > maxVal) arr(i) else maxVal)
    }

    if (arr.isEmpty) throw new IllegalArgumentException("Array is empty")
    else maxArr(1, arr(0))
  }

  // general approch
  def maxEleInArray(arr: Array[Int]): Int = {
    if (arr.isEmpty) throw new IllegalArgumentException("Array is empty")

    var maxVal = arr(0)
    for (i <- 1 until arr.length) {
      if (arr(i) > maxVal) {
        maxVal = arr(i)
      }
    }
    maxVal
  }

  def main(args: Array[String]): Unit = {
    val nums = Array(5, 9, 3, 7, 2)
    println(s"Maximum element is: ${maxInArray(nums)}")

    val n = Array(6, 78, 9, 45)
    println("Maximum element is: " + maxEleInArray(n))
  }
}
