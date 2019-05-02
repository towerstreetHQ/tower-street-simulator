package io.towerstreet.attacksimulator.services.scoring

import java.time.{Duration, LocalDateTime}

import io.towerstreet.attacksimulator.models.Scoring.{CombinedScoringParameters, TaskWithResult, UrlTestResultWitness}
import io.towerstreet.attacksimulator.models.UrlTest.UrlTaskParameters
import io.towerstreet.attacksimulator.services.scoring.UrlTestCalculatorService.{ConnectionResult, Timeouts}
import io.towerstreet.slick.models.generated.attacksimulator.Model._
import io.towerstreet.slick.models.generated.scoring.Model.{ScoringDefinition, ScoringDefinitionId, ScoringOutcomeId}
import io.towerstreet.slick.models.scoring.enums.{ScoringCategory, ScoringResultType, ScoringType}
import io.towerstreet.testhelpers.WithApplicationSpec
import io.towerstreet.testhelpers.WithApplicationSpec.WithService
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

class UrlTestCalculatorServiceTest
  extends PlaySpec
    with FutureAwaits
    with DefaultAwaitTimeout
{
  class WithSpecApplication()
    extends WithApplicationSpec
      with WithService[UrlTestCalculatorService]

  private val timeouts = Timeouts(1000, 50, 150)

  private def getUrlTestResult(id: Int,
                               taskKey: Option[String] = None,
                               isSuccess: Boolean = true,
                               hasResult: Boolean = true,
                               hasBackendTest: Boolean = true,
                               backendTestSuccess: Boolean = true,
                               duration: Duration = Duration.ZERO,
                               backendDuration: Duration = Duration.ZERO,
                               parameters: Option[UrlTaskParameters] = None
                              ) = {
    if (hasResult) {
      val urlTest =
        if (hasBackendTest) Some(UrlTest(UrlTestId(id), None, TaskId(id), 0, if (backendTestSuccess) Some(backendDuration) else None, LocalDateTime.now()))
        else None

      TaskId(id) -> Some(TaskWithResult(
        Task(TaskId(id), RunnerId(0), taskKey.getOrElse(s"task-$id"), parameters.map(p => Json.toJson(p))),
        OutcomeTaskResult(OutcomeTaskResultId(id), SimulationOutcomeId(0), TaskId(id), isSuccess, None, None, Some(duration),
          urlTest.map(_.id)),
        urlTest
      ))
    } else {
      TaskId(id) -> None
    }
  }

  private def getUrlTestResultForTimeout(duration: Int, backendDuration: Int = 100, backendTestSuccess: Boolean = true) = {
    getUrlTestResult(1,
      duration = Duration.ofMillis(duration),
      backendDuration = Duration.ofMillis(backendDuration),
      backendTestSuccess = backendTestSuccess
    )._2.get
  }

  "UrlTestCalculatorService" should {

    "isConnectedWebserverImage" should {
      "return connected NOT ignored" in new WithSpecApplication {
        val result = getUrlTestResult(1)._2.get
        val r = service.isConnectedWebserverImage(result.outcome, result.urlTest)

        r mustBe ConnectionResult(true, false)
      }
      "return connected ignored if backend can't connect" in new WithSpecApplication {
        val result = getUrlTestResult(1, hasBackendTest = false)._2.get
        val r = service.isConnectedWebserverImage(result.outcome, result.urlTest)

        r mustBe ConnectionResult(true, true)
      }
      "return NOT connected NOT ignored if FE can't connect" in new WithSpecApplication {
        val result = getUrlTestResult(1, isSuccess = false)._2.get
        val r = service.isConnectedWebserverImage(result.outcome, result.urlTest)

        r mustBe ConnectionResult(false, false)
      }
    }

    "isConnectedWebserverNoImage" should {
      "return connected NOT ignored" in new WithSpecApplication {
        val result = getUrlTestResultForTimeout(200)
        val r = service.isConnectedWebserverNoImage(result.outcome, Some(timeouts), result.urlTest)

        r mustBe ConnectionResult(true, false)
      }
      "return NOT connected NOT ignored if duration is shorter than reset" in new WithSpecApplication {
        val result = getUrlTestResultForTimeout(20)
        val r = service.isConnectedWebserverNoImage(result.outcome, Some(timeouts), result.urlTest)

        r mustBe ConnectionResult(false, false)
      }
      "return NOT connected NOT ignored if duration is longer than half of max timeout" in new WithSpecApplication {
        val result = getUrlTestResultForTimeout(600)
        val r = service.isConnectedWebserverNoImage(result.outcome, Some(timeouts), result.urlTest)

        r mustBe ConnectionResult(false, false)
      }
      "return NOT connected NOT ignored if duration is longer than base + backend connection" in new WithSpecApplication {
        val result = getUrlTestResultForTimeout(400)
        val r = service.isConnectedWebserverNoImage(result.outcome, Some(timeouts), result.urlTest)

        r mustBe ConnectionResult(false, false)
      }
      "return connected ignored if not connected by backend" in new WithSpecApplication {
        val result = getUrlTestResultForTimeout(200, backendTestSuccess = false)
        val r = service.isConnectedWebserverNoImage(result.outcome, Some(timeouts), result.urlTest)

        r mustBe ConnectionResult(false, true)
      }
      "return connected ignored if missing timeouts" in new WithSpecApplication {
        val result = getUrlTestResultForTimeout(200, backendTestSuccess = false)
        val r = service.isConnectedWebserverNoImage(result.outcome, None, result.urlTest)

        r mustBe ConnectionResult(false, true)
      }
    }

    "isConnectedNonWebserver" should {
      "return connected NOT ignored" in new WithSpecApplication {
        val result = getUrlTestResultForTimeout(200)
        val r = service.isConnectedNonWebserver(result.outcome, Some(timeouts))

        r mustBe ConnectionResult(true, false)
      }
      "return NOT connected NOT ignored if duration is longer than half of max timeout" in new WithSpecApplication {
        val result = getUrlTestResultForTimeout(600)
        val r = service.isConnectedNonWebserver(result.outcome, Some(timeouts))

        r mustBe ConnectionResult(false, false)
      }
      "return connected ignored if missing timeouts" in new WithSpecApplication {
        val result = getUrlTestResultForTimeout(200, backendTestSuccess = false)
        val r = service.isConnectedNonWebserver(result.outcome, None)

        r mustBe ConnectionResult(false, true)
      }
    }

    "webserver with image routes" should {
      "mark result as success if all tasks passed (all required)" in new WithSpecApplication{
        val r = await(service.calculateResult(
          ScoringOutcomeId(1),
          ScoringDefinition(ScoringDefinitionId(1), "", ScoringType.UrlTestResult, "", Some(Json.toJson(
            CombinedScoringParameters(successWhen = true))), "", 1, ScoringCategory.Cis),
          Seq(
            getUrlTestResult(1),
            getUrlTestResult(2),
            getUrlTestResult(3)
          )
        ))

        r.result mustBe ScoringResultType.True
        r.scoringOutcomeId mustBe ScoringOutcomeId(1)
        r.scoringDefinitionId mustBe ScoringDefinitionId(1)
        r.resultParameters mustBe Some(Json.toJson(Seq(
          UrlTestResultWitness(TaskId(1), true, true, false, Some(OutcomeTaskResultId(1)), Some(UrlTestId(1))),
          UrlTestResultWitness(TaskId(2), true, true, false, Some(OutcomeTaskResultId(2)), Some(UrlTestId(2))),
          UrlTestResultWitness(TaskId(3), true, true, false, Some(OutcomeTaskResultId(3)), Some(UrlTestId(3)))
        )))
      }

      "mark result as fail if of one task failed (all required)" in new WithSpecApplication{
        val r = await(service.calculateResult(
          ScoringOutcomeId(1),
          ScoringDefinition(ScoringDefinitionId(1), "", ScoringType.UrlTestResult, "", Some(Json.toJson(
            CombinedScoringParameters(successWhen = true))), "", 1, ScoringCategory.Cis),
          Seq(
            getUrlTestResult(1),
            getUrlTestResult(2),
            getUrlTestResult(3, isSuccess = false)
          )
        ))

        r.result mustBe ScoringResultType.False
        r.scoringOutcomeId mustBe ScoringOutcomeId(1)
        r.scoringDefinitionId mustBe ScoringDefinitionId(1)
        r.resultParameters mustBe Some(Json.toJson(Seq(
          UrlTestResultWitness(TaskId(1), true, true, false, Some(OutcomeTaskResultId(1)), Some(UrlTestId(1))),
          UrlTestResultWitness(TaskId(2), true, true, false, Some(OutcomeTaskResultId(2)), Some(UrlTestId(2))),
          UrlTestResultWitness(TaskId(3), false, true, false, Some(OutcomeTaskResultId(3)), Some(UrlTestId(3)))
        )))
      }

      "mark result as success if of one task failed (needed two to pass)" in new WithSpecApplication{
        val r = await(service.calculateResult(
          ScoringOutcomeId(1),
          ScoringDefinition(ScoringDefinitionId(1), "", ScoringType.UrlTestResult, "", Some(Json.toJson(
            CombinedScoringParameters(successWhen = true, requiredHits = Some(2)))), "", 1, ScoringCategory.Cis),
          Seq(
            getUrlTestResult(1),
            getUrlTestResult(2),
            getUrlTestResult(3, isSuccess = false)
          )
        ))

        r.result mustBe ScoringResultType.True
        r.scoringOutcomeId mustBe ScoringOutcomeId(1)
        r.scoringDefinitionId mustBe ScoringDefinitionId(1)
        r.resultParameters mustBe Some(Json.toJson(Seq(
          UrlTestResultWitness(TaskId(1), true, true, false, Some(OutcomeTaskResultId(1)), Some(UrlTestId(1))),
          UrlTestResultWitness(TaskId(2), true, true, false, Some(OutcomeTaskResultId(2)), Some(UrlTestId(2))),
          UrlTestResultWitness(TaskId(3), false, true, false, Some(OutcomeTaskResultId(3)), Some(UrlTestId(3)))
        )))
      }

      "mark result as failed if of two tasks filed (needed two to pass)" in new WithSpecApplication{
        val r = await(service.calculateResult(
          ScoringOutcomeId(1),
          ScoringDefinition(ScoringDefinitionId(1), "", ScoringType.UrlTestResult, "", Some(Json.toJson(
            CombinedScoringParameters(successWhen = true, requiredHits = Some(2)))), "", 1, ScoringCategory.Cis),
          Seq(
            getUrlTestResult(1),
            getUrlTestResult(2, isSuccess = false),
            getUrlTestResult(3, isSuccess = false)
          )
        ))

        r.result mustBe ScoringResultType.False
        r.scoringOutcomeId mustBe ScoringOutcomeId(1)
        r.scoringDefinitionId mustBe ScoringDefinitionId(1)
        r.resultParameters mustBe Some(Json.toJson(Seq(
          UrlTestResultWitness(TaskId(1), true, true, false, Some(OutcomeTaskResultId(1)), Some(UrlTestId(1))),
          UrlTestResultWitness(TaskId(2), false, true, false, Some(OutcomeTaskResultId(2)), Some(UrlTestId(2))),
          UrlTestResultWitness(TaskId(3), false, true, false, Some(OutcomeTaskResultId(3)), Some(UrlTestId(3)))
        )))
      }

      "mark result as success if only backend connected tasks passed (all required)" in new WithSpecApplication{
        val r = await(service.calculateResult(
          ScoringOutcomeId(1),
          ScoringDefinition(ScoringDefinitionId(1), "", ScoringType.UrlTestResult, "", Some(Json.toJson(
            CombinedScoringParameters(successWhen = true))), "", 1, ScoringCategory.Cis),
          Seq(
            getUrlTestResult(1),
            getUrlTestResult(2),
            getUrlTestResult(3, isSuccess = false, hasBackendTest = false),
            getUrlTestResult(4, isSuccess = false, backendTestSuccess = false)
          )
        ))

        r.result mustBe ScoringResultType.True
        r.scoringOutcomeId mustBe ScoringOutcomeId(1)
        r.scoringDefinitionId mustBe ScoringDefinitionId(1)
        r.resultParameters mustBe Some(Json.toJson(Seq(
          UrlTestResultWitness(TaskId(1), true, true, false, Some(OutcomeTaskResultId(1)), Some(UrlTestId(1))),
          UrlTestResultWitness(TaskId(2), true, true, false, Some(OutcomeTaskResultId(2)), Some(UrlTestId(2))),
          UrlTestResultWitness(TaskId(3), false, false, true, Some(OutcomeTaskResultId(3)), None),
          UrlTestResultWitness(TaskId(4), false, false, true, Some(OutcomeTaskResultId(4)), Some(UrlTestId(4))),
        )))
      }

      "mark result as success and filter out missing results" in new WithSpecApplication{
        val r = await(service.calculateResult(
          ScoringOutcomeId(1),
          ScoringDefinition(ScoringDefinitionId(1), "", ScoringType.UrlTestResult, "", Some(Json.toJson(
            CombinedScoringParameters(successWhen = true))), "", 1, ScoringCategory.Cis),
          Seq(
            getUrlTestResult(1),
            getUrlTestResult(2),
            getUrlTestResult(3, hasResult = false)
          )
        ))

        r.result mustBe ScoringResultType.True
        r.scoringOutcomeId mustBe ScoringOutcomeId(1)
        r.scoringDefinitionId mustBe ScoringDefinitionId(1)
        r.resultParameters mustBe Some(Json.toJson(Seq(
          UrlTestResultWitness(TaskId(1), true, true, false, Some(OutcomeTaskResultId(1)), Some(UrlTestId(1))),
          UrlTestResultWitness(TaskId(2), true, true, false, Some(OutcomeTaskResultId(2)), Some(UrlTestId(2))),
          UrlTestResultWitness(TaskId(3), false, false, true, None, None),
        )))
      }

      "mark result as unknown if all tasks has been filtered out" in new WithSpecApplication{
        val r = await(service.calculateResult(
          ScoringOutcomeId(1),
          ScoringDefinition(ScoringDefinitionId(1), "", ScoringType.UrlTestResult, "", Some(Json.toJson(
            CombinedScoringParameters(successWhen = true))), "", 1, ScoringCategory.Cis),
          Seq(
            getUrlTestResult(1, hasResult = false),
            getUrlTestResult(2, hasResult = false),
            getUrlTestResult(3, hasResult = false)
          )
        ))

        r.result mustBe ScoringResultType.Unknown
        r.scoringOutcomeId mustBe ScoringOutcomeId(1)
        r.scoringDefinitionId mustBe ScoringDefinitionId(1)
        r.resultParameters mustBe Some(Json.toJson(Seq(
          UrlTestResultWitness(TaskId(1), false, false, true, None, None),
          UrlTestResultWitness(TaskId(2), false, false, true, None, None),
          UrlTestResultWitness(TaskId(3), false, false, true, None, None)
        )))
      }

      "mark result as unknown if two tasks has been filtered out (needed two to pass)" in new WithSpecApplication{
        val r = await(service.calculateResult(
          ScoringOutcomeId(1),
          ScoringDefinition(ScoringDefinitionId(1), "", ScoringType.UrlTestResult, "", Some(Json.toJson(
            CombinedScoringParameters(successWhen = true, requiredHits = Some(2)))), "", 1, ScoringCategory.Cis),
          Seq(
            getUrlTestResult(1),
            getUrlTestResult(2, hasResult = false),
            getUrlTestResult(3, hasResult = false)
          )
        ))

        r.result mustBe ScoringResultType.Unknown
        r.scoringOutcomeId mustBe ScoringOutcomeId(1)
        r.scoringDefinitionId mustBe ScoringDefinitionId(1)
        r.resultParameters mustBe Some(Json.toJson(Seq(
          UrlTestResultWitness(TaskId(1), true, true, false, Some(OutcomeTaskResultId(1)), Some(UrlTestId(1))),
          UrlTestResultWitness(TaskId(2), false, false, true, None, None),
          UrlTestResultWitness(TaskId(3), false, false, true, None, None)
        )))
      }

      "mark result as success if tasks of all types passed" in new WithSpecApplication {
        val r = await(service.calculateResult(
          ScoringOutcomeId(1),
          ScoringDefinition(ScoringDefinitionId(1), "", ScoringType.UrlTestResult, "", Some(Json.toJson(
            CombinedScoringParameters(successWhen = true))), "", 1, ScoringCategory.Cis),
          Seq(
            getUrlTestResult(10, taskKey = Some(UrlTestCalculatorService.BaselineKey), duration = Duration.ofMillis(150)),
            getUrlTestResult(11, taskKey = Some(UrlTestCalculatorService.MaxTimeoutKey), duration = Duration.ofMillis(1000)),
            getUrlTestResult(12, taskKey = Some(UrlTestCalculatorService.ResetTimeoutKey), duration = Duration.ofMillis(50)),

            getUrlTestResult(1),
            getUrlTestResult(2,
              duration = Duration.ofMillis(200),
              backendDuration = Duration.ofMillis(100),
              parameters = Some(UrlTaskParameters("", hasImage = false)),
              isSuccess = false
            ),
            getUrlTestResult(3,
              duration = Duration.ofMillis(200),
              backendDuration = Duration.ofMillis(100),
              parameters = Some(UrlTaskParameters("", hasWebServer = false)),
              backendTestSuccess = false,
              isSuccess = false
            )
          )
        ))

        r.result mustBe ScoringResultType.True
        r.scoringOutcomeId mustBe ScoringOutcomeId(1)
        r.scoringDefinitionId mustBe ScoringDefinitionId(1)
        r.resultParameters mustBe Some(Json.toJson(Seq(
          UrlTestResultWitness(TaskId(1), true, true, false, Some(OutcomeTaskResultId(1)), Some(UrlTestId(1))),
          UrlTestResultWitness(TaskId(2), true, true, false, Some(OutcomeTaskResultId(2)), Some(UrlTestId(2))),
          UrlTestResultWitness(TaskId(3), true, false, false, Some(OutcomeTaskResultId(3)), Some(UrlTestId(3)))
        )))
      }

      "mark result as success if extra tests are ignored because missing timeouts" in new WithSpecApplication {
        val r = await(service.calculateResult(
          ScoringOutcomeId(1),
          ScoringDefinition(ScoringDefinitionId(1), "", ScoringType.UrlTestResult, "", Some(Json.toJson(
            CombinedScoringParameters(successWhen = true))), "", 1, ScoringCategory.Cis),
          Seq(
            getUrlTestResult(1),
            getUrlTestResult(2,
              duration = Duration.ofMillis(200),
              backendDuration = Duration.ofMillis(100),
              parameters = Some(UrlTaskParameters("", hasImage = false)),
              isSuccess = false
            ),
            getUrlTestResult(3,
              duration = Duration.ofMillis(200),
              backendDuration = Duration.ofMillis(100),
              parameters = Some(UrlTaskParameters("", hasWebServer = false)),
              backendTestSuccess = false,
              isSuccess = false
            )
          )
        ))

        r.result mustBe ScoringResultType.True
        r.scoringOutcomeId mustBe ScoringOutcomeId(1)
        r.scoringDefinitionId mustBe ScoringDefinitionId(1)
        r.resultParameters mustBe Some(Json.toJson(Seq(
          UrlTestResultWitness(TaskId(1), true, true, false, Some(OutcomeTaskResultId(1)), Some(UrlTestId(1))),
          UrlTestResultWitness(TaskId(2), false, true, true, Some(OutcomeTaskResultId(2)), Some(UrlTestId(2))),
          UrlTestResultWitness(TaskId(3), false, false, true, Some(OutcomeTaskResultId(3)), Some(UrlTestId(3)))
        )))
      }
    }
  }
}
