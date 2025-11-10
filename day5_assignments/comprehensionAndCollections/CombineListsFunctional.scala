object CombineListsFunctional {

  def main(args: Array[String]): Unit = {
    val students = List("Asha", "Bala", "Chitra")
    val subjects = List("Math", "Physics")

    val result = for {
      student <- students
      subject <- subjects
      if student.length >= subject.length
    } yield (student, subject)

    println(result)

    println("********Genaral basic approach*****")

    var result1 = List[(String, String)]()

    for (student <- students) {
      for (subject <- subjects) {
        if (student.length >= subject.length) {
          result1 = result1 :+ (student, subject) // append tuple to list
        }
      }
    }

    println(result1)
  }
}
