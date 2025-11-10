import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util._

object FutureRetry {

  def fetchDataFromServer(server: String): Future[String] = Future {
    if (scala.util.Random.nextBoolean()) s"Data from $server"
    else throw new RuntimeException("Server failed")
  }

  def fetchWithRetry(server: String, maxRetries: Int): Future[String] = {
    fetchDataFromServer(server).recoverWith {
      case _ if maxRetries > 0 => fetchWithRetry(server, maxRetries - 1)
    }
  }

  def main(args: Array[String]): Unit = {
    fetchWithRetry("Server-1", 3).onComplete {
      case Success(data) => println(s"Success: $data")
      case Failure(ex)   => println(s"Failed after retries: $ex")
    }

    Thread.sleep(2000)
  }
}
