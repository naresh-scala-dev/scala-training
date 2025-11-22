package repositories

import models.{Allocation, AllocationTable}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AllocationRepository @Inject()(
                                      protected val dbConfigProvider: DatabaseConfigProvider
                                    )(implicit ec: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  private val allocations = TableQuery[AllocationTable]

  def getOverdue(now: java.sql.Timestamp): Future[Seq[Allocation]] = {
    val query = allocations.filter(a =>
      (a.expectedReturn < now) &&
        (a.returnedAt.isEmpty) &&
        (a.reminderSent === false)
    )
    db.run(query.result)
  }

  def markReminderSent(id: Long): Future[Int] = {
    val query = allocations.filter(_.id === id).map(_.reminderSent).update(true)
    db.run(query)
  }
}
