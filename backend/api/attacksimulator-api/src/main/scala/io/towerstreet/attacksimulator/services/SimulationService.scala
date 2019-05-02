package io.towerstreet.attacksimulator.services

import java.time.{Duration, LocalDateTime}
import java.util.UUID

import akka.actor.ActorRef
import com.typesafe.config.Config
import io.towerstreet.attacksimulator.constants.UrlTestConstants
import io.towerstreet.attacksimulator.dao._
import io.towerstreet.attacksimulator.exceptions.{OutcomeAlreadyFinishedException, OutcomeNotFoundByTokenException, SimulationNotFoundByTokenException, TaskNotFoundByIdException}
import io.towerstreet.attacksimulator.models.ClientRequests
import io.towerstreet.attacksimulator.models.Simulations.{StartSimulationResponse, TestResult}
import io.towerstreet.attacksimulator.services.helpers.{WithGetOutcome, WithGetTasks, WithLogClientErrorRequest}
import io.towerstreet.exceptions.TowerstreetDAOException.EntityNotFoundException
import io.towerstreet.logging.Logging
import io.towerstreet.services.helpers.{DBIOUtils, WithDbTransaction}
import io.towerstreet.slick.models.attacksimulator.enums.ClientRequestStatus
import io.towerstreet.slick.models.generated.attacksimulator.Model
import io.towerstreet.slick.models.generated.attacksimulator.Model._
import io.towerstreet.slick.models.generated.scoring.Model.{ScoringOutcome, ScoringOutcomeId, SimulationOutcomeForScoring}
import javax.inject.{Inject, Named, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import play.api.mvc.Request
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SimulationService @Inject()(config: Config,
                                  val dbConfigProvider: DatabaseConfigProvider,
                                  simulationDAO: SimulationDAO,
                                  val simulationOutcomeDAO: SimulationOutcomeDAO,
                                  val clientRequestDAO: ClientRequestDAO,
                                  val taskDAO: TaskDAO,
                                  urlTestDAO: UrlTestDAO,
                                  scoringDAO: ScoringDAO,

                                  @Named("scoringActor") scoringActor: ActorRef,
                                 )
                                 (implicit ec: ExecutionContext)
  extends Logging
    with WithLogClientErrorRequest
    with WithGetTasks
    with WithGetOutcome
    with WithDbTransaction
    with DBIOUtils
{
  private val validTestDelay = config.getDuration("io.towerstreet.attacksimulator.urlTest.validTestDelay")
  private val finishedSimulationsScoringEnabled = config.getBoolean("io.towerstreet.attacksimulator.scoring.finishedSimulationsScoringEnabled")

  def startSimulation(token: UUID, request: Request[_]): Future[StartSimulationResponse] = {
    logger.info(s"Starting simulation with token $token")

    val f = for {
      (simulation, tasks) <- simulationDAO.getSimulationDefinitionByToken(token)

      // Create outcome object for simulation + request object for logging
      outcome = SimulationOutcome(SimulationOutcomeId(0), simulation.id, UUID.randomUUID(), LocalDateTime.now(),
        lastPingAt = LocalDateTime.now())
      clientRequest = ClientRequests.createRequest(request, ClientRequestStatus.SimulationStarted,
        Some(token), Some(simulation))

      _ <- simulationOutcomeDAO.insertWithRequest(outcome, clientRequest)
    } yield StartSimulationResponse(outcome, tasks)

    f.recoverWith[StartSimulationResponse] {
      case _:EntityNotFoundException[_] =>
        logger.error(s"Error starting simulation - simulation not found token: $token")
        logErrorRequest(SimulationNotFoundByTokenException(token), request,
          ClientRequestStatus.SimulationNotFound, Some(token))
    }
  }

  def simulationPing(outcomeToken: UUID): Future[Int] = {
    logger.debug(s"Updating simulation ping for outcomeToken $outcomeToken")
    simulationOutcomeDAO.updateLastPingAt(outcomeToken, LocalDateTime.now())
  }

  def finishSimulation(outcomeToken: UUID, request: Request[_]): Future[SimulationOutcome] = {
    logger.info(s"Finishing simulation with outcomeToken $outcomeToken")

    val f =
      DbTransaction {
        for {
          // Ensure existence of unfinished outcome
          (outcome, simulation) <- getRunningOutcomeWithSimulationQuery(outcomeToken, "Error finishing simulation")

          // Marking simulation as finished
          _ <- simulationOutcomeDAO.finishRunningSimulationQuery(outcome.id, LocalDateTime.now())

          // Request logging
          _ <- clientRequestDAO.insertQuery(ClientRequests.createRequestWithIds(request,
            ClientRequestStatus.OutcomeFinished, Some(outcomeToken), outcomeId = Some(outcome.id)))

          // Persist data for scoring if it is enabled by config
          // Actor is called outside of DbTransaction block because it sometimes caused that actor was started before
          // transaction was committed
          scoringOutcome <- DBIOUtils.maybeAction(finishedSimulationsScoringEnabled) {
            scoringDAO.insertScoringOutcome(ScoringOutcome(ScoringOutcomeId(0), simulation.customerAssessmentId,
              Some(outcome.id), LocalDateTime.now()))
          }
        } yield (outcome, simulation, scoringOutcome)
      }
        // Call actor when everything is persisted in DB
        .map(triggerScoring)

    f.recoverWith {
      case e: OutcomeNotFoundByTokenException =>
        logger.error(s"Error finishing simulation - simulation outcome not found, token: $outcomeToken")
        logErrorRequest(e, request, ClientRequestStatus.OutcomeNotFound, Some(outcomeToken))

      case e: OutcomeAlreadyFinishedException =>
        logger.error(s"Error finishing simulation - simulation outcome was already finished, token: $outcomeToken")
        logErrorRequest(e, request, ClientRequestStatus.OutcomeAlreadyFinished, Some(outcomeToken), outcomeId = Some(e.outcomeId))
    }
  }

  def storeTestResult(outcomeToken: UUID, testId: TaskId, body: TestResult, request: Request[_]): Future[OutcomeTaskResult] = {
    logger.info(s"Retrieving test result for token: $outcomeToken and task: $testId")

    for {
      (outcome, task, runner) <- getOutcomeAndTaskWithRunner(outcomeToken, testId, request, "Error storing test result")

      // Retrieve URL test for tasks
      urlTest <-
        if (UrlTestConstants.UrlTestTasks.contains(runner.runnerName)) {
          urlTestDAO.findResultForTask(task.id, LocalDateTime.now().minus(validTestDelay))
        } else {
          Future.successful(None)
        }

      testResult = OutcomeTaskResult(OutcomeTaskResultId(0), outcome.id,
        task.id, body.result, body.message, body.obj, body.duration.map(d => Duration.ofMillis(d.toLong)), urlTest.map(_.id))

      // Request logging
      clientRequest = ClientRequests.createRequest(request, ClientRequestStatus.TestResult, Some(outcomeToken), outcome = Some(outcome))

      result <- simulationOutcomeDAO.storeTestResult(testResult, clientRequest)
    } yield result
  }

  private def getOutcomeAndTaskWithRunner(outcomeToken: UUID,
                                            taskId: TaskId,
                                            request: Request[_],
                                            errorMessage: String
                                           )(implicit ec: ExecutionContext): Future[(Model.SimulationOutcome, Model.Task, Model.Runner)] = {
    for {
      // Find outcome by provided token
      outcome <- getOutcome(outcomeToken, request, errorMessage)

      // Find task by provided key
      (task, runner) <- taskDAO.getTaskWithRunnerById(taskId)
        .recoverWith {
          case _:EntityNotFoundException[_] =>
            logger.error(s"$errorMessage - task not found taskKey: $taskId")
            logErrorRequest(TaskNotFoundByIdException(taskId), request,
              ClientRequestStatus.MissingTasks, Some(outcomeToken), outcomeId = Some(outcome.id))
        }
    } yield (outcome, task, runner)
  }

  private def getRunningOutcomeWithSimulationQuery(outcomeToken: UUID,
                                                   errorMessage: String
                                                  )(implicit ec: ExecutionContext): DBIO[(SimulationOutcome, Simulation)] = {
    // Find outcome by provided token
    simulationOutcomeDAO.getOutcomeWithSimulationByTokenQuery(outcomeToken)
      .flatMap {
        case r@(outcome, _) =>
          // Test if outcome hasn't been already finished
          outcome.finishedAt.fold(DBIO.successful(r)) { _ =>
            DBIO.failed(OutcomeAlreadyFinishedException(outcomeToken, outcome.id))
          }
      }
      .transformError {
        case _:EntityNotFoundException[_] => OutcomeNotFoundByTokenException(outcomeToken)
      }
  }

  def triggerScoring: PartialFunction[(SimulationOutcome, Simulation, Option[ScoringOutcome]), SimulationOutcome] = {
    case (outcome, simulation, Some(scoringOutcome)) =>
      // Trigger actor
      scoringActor ! SimulationOutcomeForScoring(outcome.id, outcome.createdAt, outcome.finishedAt,
        simulation.templateId, Some(scoringOutcome.id), Some(scoringOutcome.queuedAt),
        customerAssessmentId = simulation.customerAssessmentId)
      // And return outcome
      outcome

    case (outcome, _, None) => outcome
  }
}
