object DelayedGreeter {

  def delayedMessage(delayMs: Int)(message: String): Unit = {
    Thread.sleep(delayMs)
    println(message)
  }

  def main(args: Array[String]): Unit = {
    val oneSecondSay: String => Unit = delayedMessage(1000)

    oneSecondSay("Hello!")
    oneSecondSay("How are you?")
    oneSecondSay("Goodbye!")
  }
}
