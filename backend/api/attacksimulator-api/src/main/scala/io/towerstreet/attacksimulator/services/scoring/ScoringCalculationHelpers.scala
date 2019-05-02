package io.towerstreet.attacksimulator.services.scoring

import java.time.LocalDateTime

import io.towerstreet.attacksimulator.models.Scoring.CombinedScoringParameters
import io.towerstreet.logging.Logging
import io.towerstreet.slick.models.generated.scoring.Model.{ScoringDefinition, ScoringOutcomeId, ScoringResult, ScoringResultId}
import io.towerstreet.slick.models.scoring.enums.ScoringResultType
import play.api.libs.json._

import scala.concurrent.Future

trait ScoringCalculationHelpers {
  this: Logging =>

  /**
    * Calculates overall result out off scoring witnesses - set of all tasks with predicates whether they cause test
    * success or failure.
    *
    * If number of included tests is ZERO or lower than requiredHits parameter then overall result of test is marked
    * as unknown.
    *
    * @param includeFunc This function is applied to include/exclude tasks from being evaluated. (e.g. not performed
    *                    tasks, tasks with missing data, ...).
    * @param successFunc This function is applied to included tasks to determine whether task is considered as
    *                    success or failure.
    */
  protected def evaluateCombinedResults[T](scoringOutcomeId: ScoringOutcomeId,
                                         scoringDefinition: ScoringDefinition,
                                         witnesses: Seq[T])
                                        (includeFunc: T => Boolean)
                                        (successFunc: T => Boolean)
                                        (implicit writes: Writes[T]): Future[ScoringResult] = {
    // Get parameters for this test case
    val parameters = parseScoringParameters(scoringDefinition, CombinedScoringParameters())

    // Remove excluded test tasks (e.g. not performed tests)
    val backendConnected = witnesses.filter(includeFunc)
    // Get successful tasks
    val successConnections = backendConnected.filter(t => successFunc(t) == parameters.successWhen)

    // Can we proceed with scoring?
    val hasEnoughTasks = parameters.requiredHits.fold(backendConnected.nonEmpty)(_ <= backendConnected.size)

    val result =
      if (hasEnoughTasks) {
        // If definition specified required hits then only that number of tests needs to be successful
        // If required hits are empty then ALL connections needs to be ok
        val isSuccess = parameters.requiredHits.fold(successConnections.size == backendConnected.size)(_ <= successConnections.size)
        ScoringResultType(isSuccess)
      } else {
        // Too many tasks has been filtered out because they can't be connected from backend OR missing in test
        // (so we don't know anything)
        ScoringResultType.Unknown
      }

    Future.successful(ScoringResult(ScoringResultId(0), scoringDefinition.id, scoringOutcomeId, LocalDateTime.now(), result,
      Some(Json.toJson(witnesses))))
  }

  protected def parseWithDefault[T](json: Option[JsValue], default: T)(implicit rds: Reads[T]): T = {
    json.flatMap {
      _.validate[T] match {
        case JsSuccess(v, _) => Some(v)
        case e: JsError =>
          logger.warn(s"Can't de-serialize parameters, using defaults, error: $e")
          None
      }
    }.getOrElse(default)
  }

  protected def parseScoringParameters[T](scoringDefinition: ScoringDefinition, default: T)(implicit rds: Reads[T]): T = {
    scoringDefinition.parameters.map {
      _.validate[T] match {
        case JsSuccess(v, _) => v

        case e: JsError =>
          logger.warn(s"Can't de-serialize scoring definition parameters, using defaults, definitionId: ${scoringDefinition.id}, error: $e")
          default
      }
    }.getOrElse(default)
  }
}
