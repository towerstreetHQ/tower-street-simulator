package io.towerstreet.attacksimulator.dao

import io.towerstreet.dao.{AbstractDAO, DAOHelpers}
import io.towerstreet.slick.models.generated.attacksimulator.Model.{Runner, Task, TaskId}
import io.towerstreet.slick.models.generated.attacksimulator.Tables
import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaskDAO @Inject()(val dbConfigProvider: DatabaseConfigProvider)
                       (implicit ec: ExecutionContext)
  extends AbstractDAO
    with DAOHelpers
{
  import api._
  import implicits._

  def getTasksByKeys(keys: Set[String]): Future[Seq[Task]] = {
    val q = Tables.TaskTable.filter(_.taskKey.inSetBind(keys)).result
    db.run(q)
  }

  @deprecated("Use DBIO variant getTasksByIdsQuery", "v0.1.4")
  def getTasksByIds(ids: Set[TaskId]): Future[Seq[Task]] = {
    val q = Tables.TaskTable.filter(_.id.inSetBind(ids)).result
    db.run(q)
  }

  def getTasksByRunners(runnerNames: Set[String]): Future[Seq[Task]] = {
    val q = for {
      r <- Tables.RunnerTable if r.runnerName.inSetBind(runnerNames)
      t <- Tables.TaskTable if r.id === t.runnerId
    } yield t
    db.run(q.result)
  }

  def getTaskById(id: TaskId): Future[Task] = {
    val q = Tables.TaskTable.filter(_.id === id)
      .result
      .mapSingleResult()

    db.run(q)
  }

  def getTaskWithRunnerById(id: TaskId): Future[(Task, Runner)] = {
    val q = Tables.TaskTable
      .join(Tables.RunnerTable).on(_.runnerId === _.id)
      .filter(_._1.id === id)
      .result
      .mapSingleResult()

    db.run(q)
  }
}
