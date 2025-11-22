package repositories

import models.Task
import slick.jdbc.JdbcProfile
import play.api.db.slick.DatabaseConfigProvider

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import java.sql.Timestamp
import scala.concurrent.duration._

import play.api.libs.Files.logger

@Singleton
class TaskRepository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig._
  import dbConfig.profile.api._

  private val tasks = TableQuery[models.TaskTable]


  /** Fetch tasks currently in progress (for progress check-ins) */
  def getAllInProgressTasks: Future[Seq[Task]] = {
    val now = new Timestamp(System.currentTimeMillis())
    db.run(
      tasks
        .filter(t => t.startTime <= now && t.endTime >= now && t.status === "InProgress")
        .result
    )
  }
  /** Fetch tasks scheduled for today (event-day alerts) */
  def getTasksForToday: Future[Seq[Task]] = {
    val now = java.time.LocalDate.now()
    val startOfDay = Timestamp.valueOf(now.atStartOfDay())
    val endOfDay = Timestamp.valueOf(now.plusDays(1).atStartOfDay().minusNanos(1))
    db.run(tasks.filter(t => t.startTime >= startOfDay && t.startTime <= endOfDay).result)
  }


  def getTasksWithin(start: Timestamp, end: Timestamp): Future[Seq[Task]] = {
    val query = tasks.filter(t =>
      t.startTime >= start &&
        t.startTime <= end &&
        t.status === "Pending"
    )
    db.run(query.result)
  }


}
