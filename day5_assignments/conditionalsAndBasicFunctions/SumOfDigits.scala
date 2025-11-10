object SumOfDigits {

  def main(args: Array[String]): Unit = {

    println(digitSum(123))
    println(genDigitsSum(79))
  }

  def digitSum(num: Int): Int = {
    if (num == 0) 0
    else {
      num % 10 + digitSum(num / 10)
    }

  }

  // general approach

  def genDigitsSum(num: Int): Int = {

    var n = num
    var sum = 0

    while (n > 0) {
      sum += n % 10
      n /= 10
    }
    sum
  }

}
