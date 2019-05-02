package io.towerstreet.attacksimulator.services.scoring

import io.towerstreet.attacksimulator.models.ExfiltrationTest.ExfiltrationTaskParameters
import io.towerstreet.attacksimulator.models.Scoring.{CombinedScoringParameters, ExfiltrationResultWitness, TaskWithResult}
import io.towerstreet.slick.models.generated.attacksimulator.Model._
import io.towerstreet.slick.models.generated.scoring.Model.{ScoringDefinition, ScoringDefinitionId, ScoringOutcomeId}
import io.towerstreet.slick.models.scoring.enums.{ScoringCategory, ScoringResultType, ScoringType}
import io.towerstreet.testhelpers.WithApplicationSpec
import io.towerstreet.testhelpers.WithApplicationSpec.WithService
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

class ExfiltrationCalculatorServiceTest
  extends PlaySpec
    with FutureAwaits
    with DefaultAwaitTimeout
{
  class WithSpecApplication()
    extends WithApplicationSpec
      with WithService[ExfiltrationCalculatorService]

  private def getBooleanTestResult(id: Int,
                                   isSuccess: Boolean = false,
                                   hasResult: Boolean = true,
                                   parameters: Option[ExfiltrationTaskParameters] = None
                                  ) = {
    if (hasResult) {
      TaskId(id) -> Some(TaskWithResult(
        Task(TaskId(id), RunnerId(0), s"task-$id", parameters.map(Json.toJson(_))),
        OutcomeTaskResult(OutcomeTaskResultId(id), SimulationOutcomeId(0), TaskId(id), isSuccess, None, None, None, None),
        None
      ))
    } else {
      TaskId(id) -> None
    }
  }

  "ExfiltrationCalculatorService" should {
    "calculateResult" should {
      "mark result as success if all tasks was blocked" in new WithSpecApplication {
        val r = await(service.calculateResult(
          ScoringOutcomeId(1),
          ScoringDefinition(ScoringDefinitionId(1), "", ScoringType.ExfiltrationResult, "", Some(Json.toJson(
            CombinedScoringParameters(requiredHits = Some(1)))), "", 1, ScoringCategory.Cis),
          Seq(
            getBooleanTestResult(1),
            getBooleanTestResult(2),
            getBooleanTestResult(3)
          )
        ))

        r.result mustBe ScoringResultType.True
        r.scoringOutcomeId mustBe ScoringOutcomeId(1)
        r.scoringDefinitionId mustBe ScoringDefinitionId(1)
        r.resultParameters mustBe Some(Json.toJson(Seq(
          ExfiltrationResultWitness(TaskId(1), false, false, 0, OutcomeTaskResultId(1)),
          ExfiltrationResultWitness(TaskId(2), false, false, 0, OutcomeTaskResultId(2)),
          ExfiltrationResultWitness(TaskId(3), false, false, 0, OutcomeTaskResultId(3))
        )))
      }

      "mark result as failed if all tasks passed" in new WithSpecApplication {
        val r = await(service.calculateResult(
          ScoringOutcomeId(1),
          ScoringDefinition(ScoringDefinitionId(1), "", ScoringType.ExfiltrationResult, "", Some(Json.toJson(
            CombinedScoringParameters(requiredHits = Some(1)))), "", 1, ScoringCategory.Cis),
          Seq(
            getBooleanTestResult(1, isSuccess = true),
            getBooleanTestResult(2, isSuccess = true),
            getBooleanTestResult(3, isSuccess = true)
          )
        ))

        r.result mustBe ScoringResultType.False
        r.scoringOutcomeId mustBe ScoringOutcomeId(1)
        r.scoringDefinitionId mustBe ScoringDefinitionId(1)
        r.resultParameters mustBe Some(Json.toJson(Seq(
          ExfiltrationResultWitness(TaskId(1), true, false, 0, OutcomeTaskResultId(1)),
          ExfiltrationResultWitness(TaskId(2), true, false, 0, OutcomeTaskResultId(2)),
          ExfiltrationResultWitness(TaskId(3), true, false, 0, OutcomeTaskResultId(3))
        )))
      }

      "mark result as success if one was blocked and test file passed" in new WithSpecApplication {
        val r = await(service.calculateResult(
          ScoringOutcomeId(1),
          ScoringDefinition(ScoringDefinitionId(1), "", ScoringType.ExfiltrationResult, "", Some(Json.toJson(
            CombinedScoringParameters(requiredHits = Some(1)))), "", 1, ScoringCategory.Cis),
          Seq(
            getBooleanTestResult(1, isSuccess = true, parameters = Some(ExfiltrationTaskParameters(isTestFile = true))),
            getBooleanTestResult(2, parameters = Some(ExfiltrationTaskParameters(testFileKey = Some("task-1"))))
          )
        ))

        r.result mustBe ScoringResultType.True
        r.scoringOutcomeId mustBe ScoringOutcomeId(1)
        r.scoringDefinitionId mustBe ScoringDefinitionId(1)
        r.resultParameters mustBe Some(Json.toJson(Seq(
          ExfiltrationResultWitness(TaskId(2), false, false, 0, OutcomeTaskResultId(2)),
        )))
      }

      "mark result as unknown if test file was blocked" in new WithSpecApplication {
        val r = await(service.calculateResult(
          ScoringOutcomeId(1),
          ScoringDefinition(ScoringDefinitionId(1), "", ScoringType.ExfiltrationResult, "", Some(Json.toJson(
            CombinedScoringParameters(requiredHits = Some(1)))), "", 1, ScoringCategory.Cis),
          Seq(
            getBooleanTestResult(1, parameters = Some(ExfiltrationTaskParameters(isTestFile = true))),
            getBooleanTestResult(2, parameters = Some(ExfiltrationTaskParameters(testFileKey = Some("task-1"))))
          )
        ))

        r.result mustBe ScoringResultType.Unknown
        r.scoringOutcomeId mustBe ScoringOutcomeId(1)
        r.scoringDefinitionId mustBe ScoringDefinitionId(1)
        r.resultParameters mustBe Some(Json.toJson(Seq(
          ExfiltrationResultWitness(TaskId(2), false, true, 0, OutcomeTaskResultId(2)),
        )))
      }
    }
  }
}
