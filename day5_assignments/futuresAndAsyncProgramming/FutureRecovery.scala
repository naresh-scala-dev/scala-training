import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util._
import scala.concurrent.duration._

object FutureRecovery {

  def riskyOperation(): Future[Int] = Future {
    val n = scala.util.Random.nextInt(3)
    if (n == 0) throw new RuntimeException("Failed!")
    n
  }

  def main(args: Array[String]): Unit = {

    val recovered = riskyOperation().recover { case _: Throwable =>
      -1
    }

    recovered.onComplete {
      case Success(value) => println(s"Recover result: $value")
      case Failure(ex)    => println(s"Recover failed: $ex")
    }

    val recoverWithRetry = riskyOperation().recoverWith { case _: Throwable =>
      riskyOperation()
    }

    recoverWithRetry.onComplete {
      case Success(value) => println(s"RecoverWith result: $value")
      case Failure(ex)    => println(s"RecoverWith failed: $ex")
    }

    Thread.sleep(2000)
  }
}
