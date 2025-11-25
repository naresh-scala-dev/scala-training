package repositories

import models.Task
import slick.jdbc.JdbcProfile
import play.api.db.slick.DatabaseConfigProvider

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import java.sql.Timestamp

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

  /** Fetch tasks within a time window (for reminders) */
  def getTasksWithin(start: Timestamp, end: Timestamp): Future[Seq[Task]] = {
    val query = tasks.filter(t =>
      t.startTime >= start &&
        t.startTime <= end &&
        t.status === "Pending"
    )
    db.run(query.result)
  }

  /** Fetch tasks for reminders where reminderSent = false */
  def getPendingReminders(start: Timestamp, end: Timestamp): Future[Seq[Task]] = {
    val query = tasks.filter(t =>
      t.startTime >= start &&
        t.startTime <= end &&
        t.status === "Pending" &&
        t.reminderSent === false
    )
    db.run(query.result)
  }

  /** Fetch tasks for event-day alerts where eventDayAlertSent = false */
  def getPendingEventDayAlerts(): Future[Seq[Task]] = {
    val now = java.time.LocalDate.now()
    val startOfDay = Timestamp.valueOf(now.atStartOfDay())
    val endOfDay = Timestamp.valueOf(now.plusDays(1).atStartOfDay().minusNanos(1))
    val query = tasks.filter(t =>
      t.startTime >= startOfDay &&
        t.startTime <= endOfDay &&
        t.eventDayAlertSent === false
    )
    db.run(query.result)
  }

  /** Mark reminder as sent */
  def markReminderSent(taskId: Long): Future[Int] = {
    val query = tasks.filter(_.id === taskId).map(_.reminderSent).update(true)
    db.run(query)
  }

  /** Mark event-day alert as sent */
  def markEventDayAlertSent(taskId: Long): Future[Int] = {
    val query = tasks.filter(_.id === taskId).map(_.eventDayAlertSent).update(true)
    db.run(query)
  }
}
