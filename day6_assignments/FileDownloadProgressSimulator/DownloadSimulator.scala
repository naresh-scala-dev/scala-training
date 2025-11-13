object DownloadSimulator {
  def main(args: Array[String]): Unit = {

    val download1 = new DownloadTask("FileA.zip", 500)
    val download2 = new DownloadTask("FileB.mp4", 300)
    val download3 = new DownloadTask("FileC.pdf", 700)

    // Start all threads
    download1.start()
    download2.start()
    download3.start()

    // Optional: wait for all downloads to finish
    download1.join()
    download2.join()
    download3.join()

    println("All downloads completed!")
  }
}
