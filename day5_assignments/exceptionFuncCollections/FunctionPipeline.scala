object FunctionPipeline {

  def main(args: Array[String]): Unit = {

    val trimSpaces: String => String = _.trim
    val stringToInt: String => Int = _.toInt
    val doubleValue: Int => Int = _ * 2

    val pipelineCompose = doubleValue.compose(stringToInt).compose(trimSpaces)
    val pipelineAndThen = trimSpaces.andThen(stringToInt).andThen(doubleValue)

    val input = " 21 "

    println(s"Using compose: ${pipelineCompose(input)}")
    println(s"Using andThen: ${pipelineAndThen(input)}")

    val swappedPipeline = stringToInt.compose(trimSpaces).andThen(doubleValue)
    println(s"Swapped pipeline: ${swappedPipeline(input)}")
  }
}
