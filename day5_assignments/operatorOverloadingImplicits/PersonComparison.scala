case class Person(name: String, age: Int)

object PersonComparison {

  implicit class PersonOps(person: Person) {
    def <(other: Person): Boolean = person.age < other.age
    def >(other: Person): Boolean = person.age > other.age
    def <=(other: Person): Boolean = person.age <= other.age
    def >=(other: Person): Boolean = person.age >= other.age
  }

  def main(args: Array[String]): Unit = {
    val person1 = Person("Ravi", 25)
    val person2 = Person("Meena", 30)

    println(person1 < person2)
    println(person1 >= person2)

    if (person1 > person2)
      println(s"${person1.name} is older")
    else
      println(s"${person2.name} is older")
  }
}
