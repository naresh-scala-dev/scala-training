object SentencePipeline {

  val trimSpaces: String => String = _.trim
  val toLower: String => String = _.toLowerCase
  val capitalizeFirst: String => String = s =>
    if (s.isEmpty) s else s.head.toUpper + s.tail.toLowerCase

  val processSentence: String => String =
    trimSpaces.andThen(toLower).andThen(capitalizeFirst)

  def main(args: Array[String]): Unit = {
    val messy = " HeLLo WOrld "
    println(processSentence(messy))
  }
}
