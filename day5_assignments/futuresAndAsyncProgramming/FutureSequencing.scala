import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util._
import java.time.Instant

object FutureSequencing {

  def task(name: String, delay: Int): Future[String] = Future {
    Thread.sleep(delay)
    s"$name done"
  }

  def main(args: Array[String]): Unit = {

    val startSeq = Instant.now()
    val sequential = task("Task1", 1000).flatMap { r1 =>
      task("Task2", 1000).flatMap { r2 =>
        task("Task3", 1000).map { r3 =>
          List(r1, r2, r3)
        }
      }
    }

    sequential.onComplete {
      case Success(results) =>
        val endSeq = Instant.now()
        println(s"Sequential results: $results")
        println(
          s"Sequential time: ${java.time.Duration.between(startSeq, endSeq).toMillis} ms"
        )
      case Failure(ex) => println(ex)
    }

    val startPar = Instant.now()
    val parallelTasks =
      List(task("Task1", 1000), task("Task2", 1000), task("Task3", 1000))
    val parallel = Future.sequence(parallelTasks)

    parallel.onComplete {
      case Success(results) =>
        val endPar = Instant.now()
        println(s"Parallel results: $results")
        println(
          s"Parallel time: ${java.time.Duration.between(startPar, endPar).toMillis} ms"
        )
      case Failure(ex) => println(ex)
    }

    Thread.sleep(4000)
  }
}
