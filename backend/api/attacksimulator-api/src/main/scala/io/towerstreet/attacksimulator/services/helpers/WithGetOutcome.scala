package io.towerstreet.attacksimulator.services.helpers

import java.util.UUID

import io.towerstreet.attacksimulator.dao.{SimulationOutcomeDAO, TaskDAO}
import io.towerstreet.attacksimulator.exceptions.{OutcomeAlreadyFinishedException, OutcomeNotFoundByTokenException, TaskNotFoundByIdException, TaskNotFoundException}
import io.towerstreet.exceptions.TowerstreetDAOException.EntityNotFoundException
import io.towerstreet.logging.Logging
import io.towerstreet.slick.models.attacksimulator.enums.ClientRequestStatus
import io.towerstreet.slick.models.generated.attacksimulator.Model
import io.towerstreet.slick.models.generated.attacksimulator.Model.TaskId
import play.api.mvc.Request

import scala.concurrent.{ExecutionContext, Future}

trait WithGetOutcome {
  this: Logging with WithLogClientErrorRequest =>

  val simulationOutcomeDAO: SimulationOutcomeDAO
  val taskDAO: TaskDAO

  protected def getOutcome(outcomeToken: UUID,
                           request: Request[_],
                           errorMessage: String
                          )(implicit ec: ExecutionContext): Future[Model.SimulationOutcome] = {
    for {
      // Find outcome by provided token
      outcome <- simulationOutcomeDAO.getOutcomeByToken(outcomeToken)
        .recoverWith {
          case _:EntityNotFoundException[_] =>
            logger.error(s"$errorMessage - simulation outcome not found token: $outcomeToken")
            logErrorRequest(OutcomeNotFoundByTokenException(outcomeToken), request,
              ClientRequestStatus.OutcomeNotFound, Some(outcomeToken))
        }

      // Test if outcome hasn't been already finished
      _ <- outcome.finishedAt.fold[Future[_]](Future.successful(None)) { _ =>
        logger.error(s"$errorMessage - simulation outcome was already finished, token: $outcomeToken")
        logErrorRequest(OutcomeAlreadyFinishedException(outcomeToken, outcome.id), request,
          ClientRequestStatus.OutcomeAlreadyFinished, Some(outcomeToken), outcomeId = Some(outcome.id))
      }
    } yield outcome
  }

  protected def getOutcomeAndTask(outcomeToken: UUID,
                                  taskId: TaskId,
                                  request: Request[_],
                                  errorMessage: String
                                 )(implicit ec: ExecutionContext): Future[(Model.SimulationOutcome, Model.Task)] = {
    for {
      // Find outcome by provided token
      outcome <- getOutcome(outcomeToken, request, errorMessage)

      // Find task by provided key
      task <- taskDAO.getTaskById(taskId)
        .recoverWith {
          case _:EntityNotFoundException[_] =>
            logger.error(s"$errorMessage - task not found taskKey: $taskId")
            logErrorRequest(TaskNotFoundByIdException(taskId), request,
              ClientRequestStatus.MissingTasks, Some(outcomeToken), outcomeId = Some(outcome.id))
        }
    } yield (outcome, task)
  }
}
