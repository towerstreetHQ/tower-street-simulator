package io.towerstreet.attacksimulator.services.scoring

import java.time.LocalDateTime

import com.typesafe.config.Config
import io.towerstreet.attacksimulator.models.Scoring.{BooleanResultWitness, CombinedScoringParameters, FailedResult, ResultState, TaskWithResult, UrlTestResultWitness}
import io.towerstreet.logging.Logging
import io.towerstreet.slick.models.generated.attacksimulator.Model.{OutcomeTaskResult, SimulationOutcomeId, TaskId, UrlTest}
import io.towerstreet.slick.models.generated.scoring.Model._
import io.towerstreet.slick.models.scoring.enums.{ScoringResultType, ScoringType}
import javax.inject.Inject
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Contains methods to perform calculation for single scoring object represented by simulation outcome and scoring
  * definition. This service contains MAIN scoring function [[calculateResult]] which routes calculation based on type
  * of the scoring definition.
  *
  * Small calculation methods could be included within this service. More complex calculations should be extracted
  * to separate service.
  */
class ScoringResultCalculatorService @Inject() (config: Config,
                                                urlTestCalculatorService: UrlTestCalculatorService,
                                                portScanCalculatorService: PortScanCalculatorService,
                                                exfiltrationCalculatorService: ExfiltrationCalculatorService
                                               )(implicit ec: ExecutionContext)
  extends Logging
    with ScoringCalculationHelpers
{

  /**
    * Performs single calculation one scoring definition for given simulation outcome. Output of this method represents
    * an entry to scoring result table.
    *
    * This method should always produce a successful Future result. In case that calculation crashes it will log
    * exception and prepare failed result which can be inserted to DB.
    */
  def calculateResult(simulationOutcomeId: SimulationOutcomeId,
                      scoringOutcomeId: ScoringOutcomeId,
                      scoringDefinition: ScoringDefinition,
                      taskOutcomes: Seq[(TaskId, Option[TaskWithResult])],
                     ): Future[ScoringResult] = {
    logger.debug(s"Starting calculation of scoring  for definition ${scoringDefinition.id}, simulationOutcomeId: " +
      s"$simulationOutcomeId, scoringDefinitionId: $scoringOutcomeId")

    val calculation = scoringDefinition.scoringType match {
      case ScoringType.BooleanResult =>
        calculateBooleanResult(scoringOutcomeId, scoringDefinition, taskOutcomes)

      case ScoringType.UrlTestResult =>
        urlTestCalculatorService.calculateResult(scoringOutcomeId, scoringDefinition, taskOutcomes)

      case ScoringType.OpenPortScanResult =>
        portScanCalculatorService.calculateOpenedPortResults(scoringOutcomeId, scoringDefinition, taskOutcomes)

      case ScoringType.FirewallPortScanResult =>
        portScanCalculatorService.calculateMissingFirewallResults(scoringOutcomeId, scoringDefinition, taskOutcomes)

      case ScoringType.ExfiltrationResult =>
        exfiltrationCalculatorService.calculateResult(scoringOutcomeId, scoringDefinition, taskOutcomes)
    }

    // Log and handle calculation error
    calculation.recover {
      case e: Throwable =>
        logger.error(s"Filed calculation of scoring for definition ${scoringDefinition.id}, simulationOutcomeId: " +
          s"$simulationOutcomeId, scoringDefinitionId: $scoringOutcomeId, error: ${e.getMessage}", e)

        ScoringResult(ScoringResultId(0), scoringDefinition.id, scoringOutcomeId, LocalDateTime.now(),
          ScoringResultType.Error, Some(Json.obj("error" -> e.getMessage)))
    }
  }

  /**
    * Calculation method to determine whether certain collection of tasks has passed or failed. Test can be
    * parametrized by [[CombinedScoringParameters]] to specify if all or certain amount of tasks is required.
    */
  def calculateBooleanResult(scoringOutcomeId: ScoringOutcomeId,
                             scoringDefinition: ScoringDefinition,
                             taskOutcomes: Seq[(TaskId, Option[TaskWithResult])]
                            ): Future[ScoringResult] = {
    // Contains overall results of all tasks used to get this scoring
    val witnesses: Seq[BooleanResultWitness] = taskOutcomes.map {
      case (id, Some(TaskWithResult(_, outcome, _))) =>
        BooleanResultWitness(id, outcome.isSuccess, Some(outcome.id))

      // Not mapped tasks are excluded
      case (id, None) =>
        BooleanResultWitness(id, isSuccess = false, None)
    }

    evaluateCombinedResults(scoringOutcomeId, scoringDefinition, witnesses)(_.outcomeTaskResultId.isDefined)(_.isSuccess)
  }

}
