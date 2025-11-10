object AnimalSounds {

  def main(args: Array[String]): Unit = {
    val animals = Map(
      "dog" -> "bark",
      "cat" -> "meow",
      "cow" -> "moo"
    )

    val updatedAnimals = animals + ("lion" -> "roar")

    println(updatedAnimals("cow"))
    println(updatedAnimals.getOrElse("tiger", "unknown"))
  }
}
