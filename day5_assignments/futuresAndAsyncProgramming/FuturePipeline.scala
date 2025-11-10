import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util._
import scala.concurrent.duration._

object FuturePipeline {

  def getUser(id: Int): Future[String] = Future {
    s"User$id"
  }

  def getOrders(user: String): Future[List[String]] = Future {
    List(s"$user-order1", s"$user-order2")
  }

  def getOrderTotal(order: String): Future[Double] = Future {
    scala.util.Random.between(10.0, 100.0)
  }

  def main(args: Array[String]): Unit = {

    val totalFuture = for {
      user <- getUser(42)
      orders <- getOrders(user)
      totals <- Future.sequence(orders.map(getOrderTotal))
    } yield totals.sum

    totalFuture.onComplete {
      case Success(total) => println(s"Total amount: $total")
      case Failure(ex)    => println(s"Failed: $ex")
    }

    Thread.sleep(2000)
  }
}
