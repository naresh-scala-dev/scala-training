object MirrorArray {

  def mirrorAnArray(array: Array[Int]): Array[Int] = {

    val arrLength = array.length
    val aggArray = new Array[Int](2 * arrLength)

    for (x <- 0 until arrLength) {
      aggArray(x) = array(x)
    }
    for (y <- 0 until arrLength) {
      aggArray(arrLength + y) = array(arrLength - 1 - y)
    }
    aggArray
  }

//or

  def mirrorArray(arr: Array[Int]): Array[Int] = {
    val n = arr.length
    // For comprehension with a calculated index for mirroring
    (for (i <- 0 until 2 * n) yield arr(math.min(i, 2 * n - 1 - i))).toArray
  }

  def main(args: Array[String]): Unit = {
    val input = Array(1, 2, 3)
    val output = mirrorArray(input)
    println(output.mkString(", "))

    println(mirrorAnArray(Array(1, 2, 3, 4)).mkString(", "))
  }
}
