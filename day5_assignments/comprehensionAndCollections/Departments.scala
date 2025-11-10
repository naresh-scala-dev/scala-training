object Departments {

  def main(args: Array[String]): Unit = {
    val departments = List(
      ("IT", List("Ravi", "Meena")),
      ("HR", List("Anita")),
      ("Finance", List("Vijay", "Kiran"))
    )

    val flatList: List[String] = for {
      (dept, employees) <- departments // pattern decomposition
      emp <- employees
    } yield s"$dept: $emp"
    flatList.foreach(println)
  }
}
