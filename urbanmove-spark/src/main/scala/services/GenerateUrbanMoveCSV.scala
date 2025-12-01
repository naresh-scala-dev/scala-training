package services

import java.io._
import scala.util.Random
import java.time._
import java.time.format.DateTimeFormatter

object GenerateUrbanMoveCSV {

  def main(args: Array[String]): Unit = {

    val outputFile = if (args.length > 0) args(0) else "urbanmove_trips.csv"
    val file = new PrintWriter(new File(outputFile))

    val areas = Array(
      "MG Road", "Indira Nagar", "Koramangala", "Whitefield",
      "Marathahalli", "HSR Layout", "BTM", "Jayanagar"
    )

    val vehicleTypes = Array("AUTO", "TAXI", "BIKE")
    val paymentMethods = Array("CASH", "UPI", "CARD")

    val rand = new Random()

    def randomDateTime(): LocalDateTime = {
      val now = LocalDateTime.now()
      now.minusMinutes(rand.nextInt(100000))
    }

    file.write(
      "tripId,driverId,vehicleType,startTime,endTime,startLocation,endLocation," +
        "distanceKm,fareAmount,paymentMethod,customerRating\n"
    )

    for (i <- 1 to 1000000) {
      val start = randomDateTime()
      val duration = rand.nextInt(50) + 5
      val end = start.plusMinutes(duration)

      val distance = (rand.nextDouble() * 15 + 1)
      val fare = distance * (rand.nextInt(10) + 10)

      val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

      file.write(
        s"$i," +
          s"${rand.nextInt(5000)}," +
          s"${vehicleTypes(rand.nextInt(3))}," +
          s"${start.format(formatter)}," +
          s"${end.format(formatter)}," +
          s"${areas(rand.nextInt(areas.length))}," +
          s"${areas(rand.nextInt(areas.length))}," +
          f"$distance%.2f," +
          f"$fare%.2f," +
          s"${paymentMethods(rand.nextInt(3))}," +
          f"${(rand.nextDouble() * 4 + 1)}%.2f\n"
      )
    }

    file.close()

    println(s"CSV generation completed $outputFile")
  }
}
