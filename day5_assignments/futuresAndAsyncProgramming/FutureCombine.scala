import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util._

object FutureCombine {

  def main(args: Array[String]): Unit = {

    val f1 = Future { Thread.sleep(1000); 10 }
    val f2 = Future { Thread.sleep(800); 20 }
    val f3 = Future { Thread.sleep(500); 30 }

    val combined: Future[String] =
      f1.flatMap { v1 =>
        f2.flatMap { v2 =>
          f3.map { v3 =>
            val sum = v1 + v2 + v3
            val avg = sum / 3
            s"Sum = $sum, Average = $avg"
          }
        }
      }

    combined.onComplete {
      case Success(result) => println(result)
      case Failure(ex)     => println(s"Failed: $ex")
    }

    Thread.sleep(2000)
  }
}
