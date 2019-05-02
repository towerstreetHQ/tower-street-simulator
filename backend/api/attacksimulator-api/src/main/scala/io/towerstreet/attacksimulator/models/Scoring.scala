package io.towerstreet.attacksimulator.models

import com.github.vitalsoftware.macros.{json, jsonDefaults}
import enumeratum.EnumEntry.Hyphencase
import enumeratum.{Enum, EnumEntry, PlayJsonEnum}
import io.towerstreet.json.TagFormat
import io.towerstreet.slick.models.attacksimulator.enums.RecordType
import io.towerstreet.slick.models.generated.attacksimulator.Model._
import io.towerstreet.slick.models.generated.scoring.Model._

object Scoring extends TagFormat {

  case class TaskWithResult(task: Task, outcome: OutcomeTaskResult, urlTest: Option[UrlTest])

  case class ScoringDefinitionWithConfig(scoringDefinition: ScoringDefinition,
                                         config: Seq[SimulationScoringConfig]
                                        )

  case class ScoringDefinitionsWithOutcome(scoringOutcome: ScoringOutcome,
                                           taskOutcomes: Seq[TaskWithResult],
                                           scoringDefinitions: Seq[ScoringDefinitionWithConfig],
                                           campaignVisitor: Option[CampaignVisitorSimulation]
                                          )

  /* ****************************
   * Scoring parameters
   */

  /**
    *
    * @param successWhen Condition when task is considered as success (can vs can't connect).
    *                    Compared against OutcomeTaskResult.isSuccess
    * @param requiredHits Number of tasks which needs to pass condition in order to mark scoring as success.
    *                     If blank then ALL tasks needs to pass the condition.
    */
  @jsonDefaults
  case class CombinedScoringParameters(successWhen: Boolean = false,
                                       requiredHits: Option[Int] = None
                                      )


  /* ****************************
   * Result case classes
   * Represent result parameters stored in DB (JSON format)
   */


  // Boolean result

  @json
  case class BooleanResultWitness(taskId: TaskId,
                                  isSuccess: Boolean,
                                  outcomeTaskResultId: Option[OutcomeTaskResultId]
                                 )

  sealed trait ResultState extends EnumEntry with Hyphencase
  object ResultState extends Enum[ResultState] with PlayJsonEnum[ResultState] {

    val values = findValues

    case object MissingResult extends ResultState
    case object WrongResult extends ResultState
  }

  @json
  case class FailedResult(taskId: TaskId,
                          isMissing: ResultState,
                          result: Option[Boolean]
                         )

  // URL test result

  @json
  case class UrlTestResultWitness(taskId: TaskId,
                                  connected: Boolean,
                                  backendConnected: Boolean,
                                  ignored: Boolean,
                                  outcomeTaskResultId: Option[OutcomeTaskResultId],
                                  urlTestId: Option[UrlTestId]
                                 )

  // Exfiltration test result

  @json
  case class ExfiltrationResultWitness(taskId: TaskId,
                                       isSuccess: Boolean,
                                       ignored: Boolean,
                                       records: Int,
                                       outcomeTaskResultId: OutcomeTaskResultId,
                                       recordType: Option[RecordType] = None
                                      )

}
