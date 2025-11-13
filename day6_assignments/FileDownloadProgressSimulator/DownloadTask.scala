class DownloadTask(fileName: String, downloadSpeedMs: Long) extends Thread {

  override def run(): Unit = {
    for (progress <- 10 to 100 by 10) {
      println(s"$fileName: $progress% downloaded")
      try {
        Thread.sleep(downloadSpeedMs)
      } catch {
        case e: InterruptedException =>
          println(s"$fileName download interrupted")
      }
    }
    println(s"$fileName download completed!")
  }
}
