package io.towerstreet.attacksimulator.services.scoring

import java.time.LocalDateTime

import com.typesafe.config.Config
import io.towerstreet.attacksimulator.models.Scoring.{BooleanResultWitness, CombinedScoringParameters, TaskWithResult, UrlTestResultWitness}
import io.towerstreet.slick.models.generated.attacksimulator.Model.{OutcomeTaskResultId, UrlTestId, _}
import io.towerstreet.slick.models.generated.scoring.Model._
import io.towerstreet.slick.models.scoring.enums.{ScoringCategory, ScoringResultType, ScoringType}
import io.towerstreet.testhelpers.WithApplicationSpec
import io.towerstreet.testhelpers.WithApplicationSpec.WithService
import javax.inject.Inject
import org.scalatestplus.play.PlaySpec
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.Json
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import play.inject.Bindings.bind

import scala.concurrent.{ExecutionContext, Future}

class MockedScoringResultCalculatorService @Inject()(config: Config)
                                                    (implicit ec: ExecutionContext)
  extends ScoringResultCalculatorService(config, null, null, null)
{
  var defaultCalculation: Boolean = true
  var successCalculation: Boolean = true

  override def calculateBooleanResult(scoringOutcomeId: ScoringOutcomeId,
                                      scoringDefinition: ScoringDefinition,
                                      taskOutcomes: Seq[(TaskId, Option[TaskWithResult])]
                                     ): Future[ScoringResult] =
  {
    if (successCalculation) {
      Future.successful(ScoringResult(ScoringResultId(0), scoringDefinition.id, scoringOutcomeId, LocalDateTime.now(),
        ScoringResultType.True, None))
    } else {
      Future.failed(new RuntimeException("Arbitrary failure in test"))
    }
  }
}

class ScoringResultCalculatorServiceTest
  extends PlaySpec
    with FutureAwaits
    with DefaultAwaitTimeout
{
  class WithMockedSpecApplication()
    extends WithApplicationSpec
      with WithService[ScoringResultCalculatorService]
  {
    override protected def getInjectOverrides: Seq[GuiceableModule] = Seq(
      bind(classOf[ScoringResultCalculatorService]).to(classOf[MockedScoringResultCalculatorService])
    )
  }

  class WithSpecApplication()
    extends WithApplicationSpec
      with WithService[ScoringResultCalculatorService]

  private def getBooleanTestResult(id: Int,
                                   isSuccess: Boolean = true,
                                   hasResult: Boolean = true
                                  ) = {
    if (hasResult) {
      TaskId(id) -> Some(TaskWithResult(
        Task(TaskId(id), RunnerId(0), s"task-$id", None),
        OutcomeTaskResult(OutcomeTaskResultId(id), SimulationOutcomeId(0), TaskId(id), isSuccess, None, None, None, None),
        None
      ))
    } else {
      TaskId(id) -> None
    }
  }

  "ScoringResultCalculatorService" should {
    "calculateResult" should {

      "successfully perform calculation" in new WithMockedSpecApplication {
        val r = await(service.calculateResult(
          SimulationOutcomeId(1),
          ScoringOutcomeId(1),
          ScoringDefinition(ScoringDefinitionId(1), "", ScoringType.BooleanResult, "", None, "", 1, ScoringCategory.Cis),
          Seq()
        ))

        r.result mustBe ScoringResultType.True
      }

      "return success even for failed calculations" in new WithMockedSpecApplication() {
        service.asInstanceOf[MockedScoringResultCalculatorService].successCalculation = false

        val r = await(service.calculateResult(
          SimulationOutcomeId(1),
          ScoringOutcomeId(1),
          ScoringDefinition(ScoringDefinitionId(1), "", ScoringType.BooleanResult, "", None, "", 1, ScoringCategory.Cis),
          Seq()
        ))

        r mustBe ScoringResult(ScoringResultId(0), ScoringDefinitionId(1), ScoringOutcomeId(1), r.createdAt,
          ScoringResultType.Error, Some(Json.obj("error" -> "Arbitrary failure in test")))
      }
    }

    "calculateBooleanResult" should {
      "mark result as success if all tasks passed (all required)" in new WithSpecApplication {
        val r = await(service.calculateBooleanResult(
          ScoringOutcomeId(1),
          ScoringDefinition(ScoringDefinitionId(1), "", ScoringType.BooleanResult, "", Some(Json.toJson(
            CombinedScoringParameters(successWhen = true))), "", 1, ScoringCategory.Cis),
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
          BooleanResultWitness(TaskId(1), true, Some(OutcomeTaskResultId(1))),
          BooleanResultWitness(TaskId(2), true, Some(OutcomeTaskResultId(2))),
          BooleanResultWitness(TaskId(3), true, Some(OutcomeTaskResultId(3)))
        )))
      }
      "mark result as fail if of one task failed (all required)" in new WithSpecApplication{
        val r = await(service.calculateBooleanResult(
          ScoringOutcomeId(1),
          ScoringDefinition(ScoringDefinitionId(1), "", ScoringType.UrlTestResult, "", Some(Json.toJson(
            CombinedScoringParameters(successWhen = true))), "", 1, ScoringCategory.Cis),
          Seq(
            getBooleanTestResult(1),
            getBooleanTestResult(2),
            getBooleanTestResult(3, isSuccess = false)
          )
        ))

        r.result mustBe ScoringResultType.False
        r.scoringOutcomeId mustBe ScoringOutcomeId(1)
        r.scoringDefinitionId mustBe ScoringDefinitionId(1)
        r.resultParameters mustBe Some(Json.toJson(Seq(
          BooleanResultWitness(TaskId(1), true, Some(OutcomeTaskResultId(1))),
          BooleanResultWitness(TaskId(2), true, Some(OutcomeTaskResultId(2))),
          BooleanResultWitness(TaskId(3), false, Some(OutcomeTaskResultId(3)))
        )))
      }

      "mark result as success if of one task failed (needed two to pass)" in new WithSpecApplication{
        val r = await(service.calculateBooleanResult(
          ScoringOutcomeId(1),
          ScoringDefinition(ScoringDefinitionId(1), "", ScoringType.UrlTestResult, "", Some(Json.toJson(
            CombinedScoringParameters(successWhen = true, requiredHits = Some(2)))), "", 1, ScoringCategory.Cis),
          Seq(
            getBooleanTestResult(1),
            getBooleanTestResult(2),
            getBooleanTestResult(3, isSuccess = false)
          )
        ))

        r.result mustBe ScoringResultType.True
        r.scoringOutcomeId mustBe ScoringOutcomeId(1)
        r.scoringDefinitionId mustBe ScoringDefinitionId(1)
        r.resultParameters mustBe Some(Json.toJson(Seq(
          BooleanResultWitness(TaskId(1), true, Some(OutcomeTaskResultId(1))),
          BooleanResultWitness(TaskId(2), true, Some(OutcomeTaskResultId(2))),
          BooleanResultWitness(TaskId(3), false, Some(OutcomeTaskResultId(3)))
        )))
      }

      "mark result as failed if of two tasks filed (needed two to pass)" in new WithSpecApplication{
        val r = await(service.calculateBooleanResult(
          ScoringOutcomeId(1),
          ScoringDefinition(ScoringDefinitionId(1), "", ScoringType.UrlTestResult, "", Some(Json.toJson(
            CombinedScoringParameters(successWhen = true, requiredHits = Some(2)))), "", 1, ScoringCategory.Cis),
          Seq(
            getBooleanTestResult(1),
            getBooleanTestResult(2, isSuccess = false),
            getBooleanTestResult(3, isSuccess = false)
          )
        ))

        r.result mustBe ScoringResultType.False
        r.scoringOutcomeId mustBe ScoringOutcomeId(1)
        r.scoringDefinitionId mustBe ScoringDefinitionId(1)
        r.resultParameters mustBe Some(Json.toJson(Seq(
          BooleanResultWitness(TaskId(1), true, Some(OutcomeTaskResultId(1))),
          BooleanResultWitness(TaskId(2), false, Some(OutcomeTaskResultId(2))),
          BooleanResultWitness(TaskId(3), false, Some(OutcomeTaskResultId(3)))
        )))
      }

      "mark result as success if only tasks with result passed (all required)" in new WithSpecApplication{
        val r = await(service.calculateBooleanResult(
          ScoringOutcomeId(1),
          ScoringDefinition(ScoringDefinitionId(1), "", ScoringType.UrlTestResult, "", Some(Json.toJson(
            CombinedScoringParameters(successWhen = true))), "", 1, ScoringCategory.Cis),
          Seq(
            getBooleanTestResult(1),
            getBooleanTestResult(2),
            getBooleanTestResult(3, hasResult = false),
          )
        ))

        r.result mustBe ScoringResultType.True
        r.scoringOutcomeId mustBe ScoringOutcomeId(1)
        r.scoringDefinitionId mustBe ScoringDefinitionId(1)
        r.resultParameters mustBe Some(Json.toJson(Seq(
          BooleanResultWitness(TaskId(1), true, Some(OutcomeTaskResultId(1))),
          BooleanResultWitness(TaskId(2), true, Some(OutcomeTaskResultId(2))),
          BooleanResultWitness(TaskId(3), false, None)),
        ))
      }

      "mark result as unknown if all tasks has been filtered out" in new WithSpecApplication{
        val r = await(service.calculateBooleanResult(
          ScoringOutcomeId(1),
          ScoringDefinition(ScoringDefinitionId(1), "", ScoringType.UrlTestResult, "", Some(Json.toJson(
            CombinedScoringParameters(successWhen = true))), "", 1, ScoringCategory.Cis),
          Seq(
            getBooleanTestResult(1, hasResult = false),
            getBooleanTestResult(2, hasResult = false),
            getBooleanTestResult(3, hasResult = false)
          )
        ))

        r.result mustBe ScoringResultType.Unknown
        r.scoringOutcomeId mustBe ScoringOutcomeId(1)
        r.scoringDefinitionId mustBe ScoringDefinitionId(1)
        r.resultParameters mustBe Some(Json.toJson(Seq(
          BooleanResultWitness(TaskId(1), false, None),
          BooleanResultWitness(TaskId(2), false, None),
          BooleanResultWitness(TaskId(3), false, None)
        )))
      }

      "mark result as unknown if two tasks has been filtered out (needed two to pass)" in new WithSpecApplication{
        val r = await(service.calculateBooleanResult(
          ScoringOutcomeId(1),
          ScoringDefinition(ScoringDefinitionId(1), "", ScoringType.UrlTestResult, "", Some(Json.toJson(
            CombinedScoringParameters(successWhen = true, requiredHits = Some(2)))), "", 1, ScoringCategory.Cis),
          Seq(
            getBooleanTestResult(1),
            getBooleanTestResult(2, hasResult = false),
            getBooleanTestResult(3, hasResult = false)
          )
        ))

        r.result mustBe ScoringResultType.Unknown
        r.scoringOutcomeId mustBe ScoringOutcomeId(1)
        r.scoringDefinitionId mustBe ScoringDefinitionId(1)
        r.resultParameters mustBe Some(Json.toJson(Seq(
          BooleanResultWitness(TaskId(1), true, Some(OutcomeTaskResultId(1))),
          BooleanResultWitness(TaskId(2), false, None),
          BooleanResultWitness(TaskId(3), false, None)
        )))
      }
    }
  }
}
