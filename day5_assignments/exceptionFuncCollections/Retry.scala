import scala.util.Random

class Retry {

  def fetchData(): Int = {
    val n = Random.nextInt(3)
    if (n == 0) throw new RuntimeException("Network fail")
    n
  }

  def retry(times: Int)(op: => Int): Option[Int] = {
    if (times <= 0) None
    else {
      try Some(op)
      catch {
        case _: Exception => retry(times - 1)(op)
      }
    }
  }
}

object RetryApp {
  def main(args: Array[String]): Unit = {
    val retryOp = new Retry
    val result = retryOp.retry(3)(retryOp.fetchData())
    println("Operation result: " + result)
  }
}
