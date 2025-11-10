object StringEnrichment {

  implicit class RichString(val str: String) extends AnyVal {
    def *(times: Int): String = str.repeat(times)
    def ~(other: String): String = s"$str $other"
  }

  def main(args: Array[String]): Unit = {
    println("Hi" * 3)
    println("Hello" ~ "World")
  }
}
