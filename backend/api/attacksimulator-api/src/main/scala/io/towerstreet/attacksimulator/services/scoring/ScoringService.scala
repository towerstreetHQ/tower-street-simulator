package io.towerstreet.attacksimulator.services.scoring

import java.time.LocalDateTime

import com.typesafe.config.Config
import io.towerstreet.attacksimulator.dao.{CampaignDAO, ScoringDAO, SimulationOutcomeDAO}
import io.towerstreet.attacksimulator.models.Scoring._
import io.towerstreet.logging.Logging
import io.towerstreet.services.helpers.{DBIOUtils, FutureUtils, WithDbTransaction}
import io.towerstreet.slick.models.generated.attacksimulator.Model._
import io.towerstreet.slick.models.generated.public.Model.CustomerId
import io.towerstreet.slick.models.generated.scoring.Model._
import io.towerstreet.slick.models.scoring.enums.{ScoringCategory, ScoringResultType}
import javax.inject.Inject
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

object ScoringService {
  case class ScoreUpdate(customerId: CustomerId, score: Int)
}

/**
  * Contains service methods to be used from scoring actors.
  */
class ScoringService @Inject() (config: Config,

                                scoringResultCalculatorService: ScoringResultCalculatorService,

                                scoringDAO: ScoringDAO,
                                simulationOutcomeDAO: SimulationOutcomeDAO,
                                campaignDAO: CampaignDAO
                               )(implicit ec: ExecutionContext)
  extends Logging
    with WithDbTransaction
{
  import ScoringService._

  private lazy val restartDelay = config.getDuration("io.towerstreet.attacksimulator.scoring.restartDelay")
  private lazy val restartLimit = config.getInt("io.towerstreet.attacksimulator.scoring.restartLimit")
  private lazy val retriesLimit = config.getInt("io.towerstreet.attacksimulator.scoring.retriesLimit")
  private lazy val calculationConcurrency = config.getInt("io.towerstreet.attacksimulator.scoring.calculationConcurrency")

  /**
    * Queries DB to obtain simulation outcomes to be scored. Tries to find newly created outcomes which hasn't been
    * processed in quick time or outcomes which failed to be processed (finish date is not set and calculation
    * started before a long time). This behavior should resolve invalid state which was created due to restart
    * of the application server.
    */
  def loadSimulationOutcomesForScoring(): Future[Seq[SimulationOutcomeForScoring]] = {
    val restartThreshold = LocalDateTime.now().plus(restartDelay)
    DbTransaction {
      for {
        simulationOutcomes <- scoringDAO.getSimulationOutcomeForScoringQuery(
          restartThreshold, restartThreshold, restartLimit, retriesLimit)

        (reRun, firstRun) = simulationOutcomes.partition(_.scoringOutcomeId.isDefined)

        _ <- DBIOUtils.maybeAction(firstRun.nonEmpty) {
          scoringDAO.insertScoringOutcomes(
            firstRun.map(o => ScoringOutcome(ScoringOutcomeId(0), o.customerAssessmentId,
              Some(o.simulationOutcomeId), LocalDateTime.now()))
          )
        }

        _ <- DBIOUtils.maybeAction(reRun.nonEmpty) {
          scoringDAO.updateQueuedAt(reRun.flatMap(_.scoringOutcomeId), LocalDateTime.now())
        }

      } yield simulationOutcomes
    }
  }

  /**
    * Performs single scoring of given simulation outcome. Pulls all related data from database and then run scoring
    * for each definition one by one. If calculation crashes then persists crash info to database.
    *
    * Method uses 2 DB transactions - to load data from DB and to persist results. Two transactions are used because
    * calculation can take some time and we don't want to block DB.
    *
    * Calculation itself is perform as sequence of future results for each scoring definition object. Future
    * calculations runs in chunks with configurable level of parallelism. They can access DB (if needed) so we need
    * to avoid running too much of them at once.
    */
  def performScoring(simulationOutcome: SimulationOutcomeForScoring): Future[ScoringDefinitionsWithOutcome] = {
    val simulationOutcomeId = simulationOutcome.simulationOutcomeId

    logger.info(s"Starting scoring simulation outcome: $simulationOutcomeId, scoring: $simulationOutcomeId")

    for {
      // Get scoring definitions and task outcomes from database
      data <- getScoringDefinitionsWithOutcome(
        simulationOutcome.simulationOutcomeId,
        simulationOutcome.templateId
      )

      scoringOutcomeId = data.scoringOutcome.id

      // Group data needed for each scoring definition
      taskOutcomeMap = data.taskOutcomes.map(t => t.task.id -> t).toMap
      definitionsForCalculation = data.scoringDefinitions.map {
        case ScoringDefinitionWithConfig(scoringDefinition, c) =>
          (scoringDefinition, c.map(c => c.taskId -> taskOutcomeMap.get(c.taskId)))
      }

      // Run calculation for each definition
      // Error is processed in calculateResult method
      results <- FutureUtils.calculateSequential(definitionsForCalculation, calculationConcurrency) {
        case (scoringDefinition, tasks) => scoringResultCalculatorService.calculateResult(
          simulationOutcomeId, scoringOutcomeId, scoringDefinition, tasks)
      }

      scoreUpdate = data.campaignVisitor.map(updateCampaignScore(_, data.scoringDefinitions, results))

      // Persist results
      _ <- finishScoring(data.scoringOutcome.id, results, scoreUpdate)
    } yield data
  }

  /**
    * Gets data to perform scoring for single simulation outcome. Loads test tasks results + scoring definition.
    */
  private[scoring] def getScoringDefinitionsWithOutcome(simulationOutcomeId: SimulationOutcomeId,
                                                        simulationTemplateId: SimulationTemplateId
                                                       ): Future[ScoringDefinitionsWithOutcome] = {
    DbTransaction {
      for {
        // Outcome with task result
        scoringOutcome <- scoringDAO.getScoringOutcomeForSimulation(simulationOutcomeId)
        taskResults <- simulationOutcomeDAO.getTasksWithResults(simulationOutcomeId)

        // Scoring definitions based on used simulation template
        scoringConfig <- scoringDAO.getScoringConfigForTemplate(simulationTemplateId)
        scoringDefinitionsIds = scoringConfig.map(_.scoringDefinitionId).toSet
        scoringDefinitions <- scoringDAO.getScoringDefinitionsByIds(scoringDefinitionsIds)

        // Get campaign visitor for this outcome ID
        visitor <- campaignDAO.getVisitorSimulation(simulationOutcomeId)
      } yield  {
        val scoringConfigByDefinitions = scoringConfig.groupBy(_.scoringDefinitionId)

        val definitionsWithConfig = scoringDefinitions.map { definition =>
          ScoringDefinitionWithConfig(definition, scoringConfigByDefinitions.getOrElse(definition.id, Seq()))
        }

        ScoringDefinitionsWithOutcome(
          scoringOutcome,
          taskResults,
          definitionsWithConfig,
          visitor
        )
      }
    }
  }

  /**
    * Persists result of single scoring - marks scoring as finished + insert all calculated results.
    */
  private def finishScoring(scoringOutcomeId: ScoringOutcomeId,
                            scoringResults: Seq[ScoringResult],
                            scoreUpdate: Option[ScoreUpdate]
                           ) = {
    DbTransaction {
      for {
        _ <- scoringDAO.insertScoringResults(scoringResults)
        _ <- scoringDAO.updateFinishedAt(scoringOutcomeId, LocalDateTime.now())
        _ <- DBIO.sequenceOption(scoreUpdate.map(s => campaignDAO.updateScoreHistogram(s.customerId, s.score)))
      } yield ()
    }
  }

  private def updateCampaignScore(visitor: CampaignVisitorSimulation,
                                  scoringDefinitions: Seq[ScoringDefinitionWithConfig],
                                  scoringResults: Seq[ScoringResult]
                                 ): ScoreUpdate = {
    // Find definitions of score object
    val scoreDefinitions = scoringDefinitions
      .filter(_.scoringDefinition.category == ScoringCategory.Score)
      .map(_.scoringDefinition.id)
      .toSet

    // Score is number of success score results
    val score = scoringResults.count(r =>
      scoreDefinitions.contains(r.scoringDefinitionId) && r.result == ScoringResultType.True)

    ScoreUpdate(visitor.customerId, score)
  }
}
