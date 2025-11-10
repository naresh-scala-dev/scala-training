object WordFrequencyCounter {
  def main(args: Array[String]): Unit = {
    val sentences = List(
      "Scala is powerful",
      "Scala is concise",
      "Functional programming is powerful"
    )

    val allWords: List[String] = for {
      sentence <- sentences
      word <- sentence.split(" ")
    } yield word
    val frequencyMap: Map[String, Int] =
      allWords.groupBy(identity).view.mapValues(_.size).toMap
    println(frequencyMap)
  }
}
