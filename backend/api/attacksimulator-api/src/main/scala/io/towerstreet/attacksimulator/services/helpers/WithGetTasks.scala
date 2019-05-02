package io.towerstreet.attacksimulator.services.helpers

import io.towerstreet.attacksimulator.dao.TaskDAO
import io.towerstreet.attacksimulator.exceptions.MissingTasksException
import io.towerstreet.logging.Logging
import io.towerstreet.slick.models.generated.attacksimulator.Model.TaskId

import scala.concurrent.{ExecutionContext, Future}

trait WithGetTasks
  extends Logging
{
  val taskDAO: TaskDAO

  protected def getTasksByKeys(taskKeys: Set[String], logMessage: String)
                              (implicit executionContext: ExecutionContext): Future[Map[String, TaskId]] = {
    if (taskKeys.nonEmpty) {
      taskDAO.getTasksByKeys(taskKeys)
        .flatMap { tasks =>
          if (tasks.size == taskKeys.size) {
            val taskMapping = tasks.map(t => t.taskKey -> t.id).toMap
            Future.successful(taskMapping)
          } else {
            // Some tasks are missing - find out which one and exception
            val existing = tasks.map(_.taskKey).toSet
            val missing = taskKeys.diff(existing).toSeq

            logger.error(s"$logMessage - tasks for simulations are not present in the database $missing")
            Future.failed(MissingTasksException(missing))
          }
        }
    } else {
      Future.successful(Map.empty[String, TaskId])
    }
  }

  protected def getTasksByIds(taskIds: Set[TaskId], logMessage: String)
                             (implicit executionContext: ExecutionContext): Future[Boolean] = {
    if (taskIds.nonEmpty) {
      taskDAO.getTasksByIds(taskIds)
        .flatMap { tasks =>
          if (tasks.size == taskIds.size) {
            Future.successful(true)
          } else {
            // Some tasks are missing - find out which one and exception
            val existing = tasks.map(_.id).toSet
            val missing = taskIds.diff(existing).toSeq

            logger.error(s"$logMessage - tasks for simulations are not present in the database $missing")
            Future.failed(MissingTasksException(missingIds = missing))
          }
        }
    } else {
      Future.successful(true)
    }
  }
}
