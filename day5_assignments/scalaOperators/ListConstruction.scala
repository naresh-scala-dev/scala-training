object ListConstruction {

  def main(args: Array[String]): Unit = {
    val myList = 1 :: 2 :: 3 :: 4 :: Nil
    println(myList)

    val anotherList = 0 :: 1 :: myList
    println(anotherList)

    val reversedList = 4 :: 3 :: 2 :: 1 :: Nil
    println(reversedList)
  }

}
