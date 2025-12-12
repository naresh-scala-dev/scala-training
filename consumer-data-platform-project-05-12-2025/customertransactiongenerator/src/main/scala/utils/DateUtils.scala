package utils

import java.time.{LocalDate, LocalDateTime, ZoneId}
import scala.util.Random

object DateUtils {
  def randomDate(start: LocalDate, end: LocalDate): LocalDate = {
    val days = java.time.temporal.ChronoUnit.DAYS.between(start, end)
    start.plusDays((Random.nextDouble() * days).toLong)
  }

  def now(): LocalDateTime = LocalDateTime.now(ZoneId.of("UTC"))
}