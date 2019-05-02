package io.towerstreet.attacksimulator.services

import java.util.UUID

import acolyte.jdbc.{QueryResult, StatementHandler, UpdateResult}
import akka.actor.{Actor, ActorRef, ActorSystem}
import akka.testkit.TestProbe
import com.google.inject.AbstractModule
import io.towerstreet.attacksimulator.exceptions.{OutcomeAlreadyFinishedException, OutcomeNotFoundByTokenException, SimulationNotFoundByTokenException, TaskNotFoundByIdException}
import io.towerstreet.attacksimulator.models.Simulations.{TestDefinitionResponse, TestResult}
import io.towerstreet.attacksimulator.testhelpers.db.Results
import io.towerstreet.attacksimulator.testhelpers.helpers.WithCheckClientRequest
import io.towerstreet.slick.models.attacksimulator.enums.ClientRequestStatus
import io.towerstreet.slick.models.generated.attacksimulator.Model.TaskId
import io.towerstreet.slick.models.generated.scoring.Model.SimulationOutcomeForScoring
import io.towerstreet.testhelpers.WithApplicationSpec.WithService
import io.towerstreet.testhelpers.db.ExecutorHandlers.{Handler, Param, Query, ~}
import io.towerstreet.testhelpers.db.{ResultHelpers, WithMockedDbApplication}
import org.scalatestplus.play.PlaySpec
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.concurrent.AkkaGuiceSupport
import play.api.libs.json.{JsNumber, JsObject, JsString}
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}

import scala.collection.mutable

class SimulationServiceTest
  extends PlaySpec
    with FutureAwaits
    with DefaultAwaitTimeout
    with WithCheckClientRequest
{
  private lazy val simulationToken = UUID.fromString("b749cda3-3a6f-4cb3-9635-ece0340a0467")
  private lazy val outcomeToken = UUID.fromString("6d942879-fb00-4e2a-b6f8-96328c723be1")
  private lazy val finishedOutcomeToken = UUID.fromString("87f9c76a-c37a-47ff-a5fc-61212cd141b0")
  private lazy val unknownToken = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff")


  class WithSpecApplication
    extends WithMockedDbApplication
      with WithService[SimulationService]
  {
    def playRequest = FakeRequest()
      .withHeaders("User-Agent" -> userAgent)

    // Disable std actors
    override protected def getDisabledModules: Seq[Class[_]] = Seq(classOf[io.towerstreet.attacksimulator.Module])

    // Mock scoring actor to test kit probe
    val actorSystem = ActorSystem("test")
    val probe = TestProbe()(actorSystem)
    override protected def getInjectOverrides: Seq[GuiceableModule] = Seq(
      bind[ActorSystem].toInstance(actorSystem),
      bind[ActorRef].qualifiedWith("scoringActor").toInstance(probe.ref)
    )

    // To check inserts and updates
    val requestMap = mutable.ArrayBuffer.empty[Seq[Any]]
    val outcomes = mutable.ArrayBuffer.empty[Seq[Any]]
    val scoringOutcomes = mutable.ArrayBuffer.empty[Seq[Any]]
    var updateCalled = false

    override lazy val handler: StatementHandler = Handler("^select ") {
      case ~(Query("""^select .* from "attacksimulator"."simulation".* where "token" = 'b749cda3""")) => Results.singleSimulation(simulationToken)
      case ~(Query("""^select .* from "attacksimulator"."simulation".* where "token"""")) => QueryResult.Nil

      case ~(Query("""^select .* from "attacksimulator"."task_for_simulation"""")) => Results.taskForSimulation

      case ~(Query("""^select .* from "attacksimulator"."simulation_outcome".*"token" = '6d942879""")) => Results.singleOutcomeWithSimulation(outcomeToken)
      case ~(Query("""^select .* from "attacksimulator"."simulation_outcome".*"token" = '87f9c76a""")) => Results.singleOutcomeWithSimulation(finishedOutcomeToken, true)
      case ~(Query("""^select .* from "attacksimulator"."simulation_outcome".*"token"""")) => QueryResult.Nil

      case ~(Query("""^select .* from.*"attacksimulator"."task".*"id" = 1""")) => Results.singleTaskWithRunner
      case ~(Query("""^select .* from.*"attacksimulator"."task".*"id" = 2""")) => QueryResult.Nil
      case ~(Param("""^select .* from "attacksimulator"."task""""), (4, 5)) => QueryResult.Nil
      case ~(Query("""^select .* from "attacksimulator"."task"""")) => Results.tasks

      case ~(Query("""^insert into "attacksimulator"."simulation_outcome"""")) => ResultHelpers.intKeyResult(1)
      case ~(Param("""^insert into "attacksimulator"."client_request""""), v@_*) =>
        requestMap += v
        ResultHelpers.intKeyResult(1)

      case ~(Query("""^update "attacksimulator"."simulation_outcome" set""")) =>
        updateCalled = true
        UpdateResult.One
      case ~(Param("""^insert into "attacksimulator"."outcome_task_result""""), v@_*) =>
        outcomes += v
        ResultHelpers.intKeyResult(1)

      case ~(Param("""^insert into "scoring"."scoring_outcome""""), v@_*) => {
        scoringOutcomes += v
        ResultHelpers.intKeyResult(v(1).asInstanceOf[Int])
      }
    }

    def expectScoringStarted() = {
      probe.expectMsgPF() {
        case _: SimulationOutcomeForScoring =>
          // Just ensure that we receive this message type
      }
    }
  }

  "SimulationServiceTest" should {

    "startSimulation" should {
      "start simulation with given token" in new WithSpecApplication {
        val r = await(service.startSimulation(simulationToken, playRequest))
        r.tests mustBe Seq(
          TestDefinitionResponse(TaskId(1), "runner-01", Some(JsObject(Map("f" -> JsString("f")))), Some("test-case-01")),
          TestDefinitionResponse(TaskId(2), "runner-01", None, None),
          TestDefinitionResponse(TaskId(3), "runner-02", Some(JsObject(Map("f" -> JsString("f")))), Some("test-case-02"))
        )

        requestMap must have size 1
        checkClientRequest(requestMap.head, ClientRequestStatus.SimulationStarted, Some(1), Some(1))
      }

      "fail to start simulation with missing token" in new WithSpecApplication {
        an [SimulationNotFoundByTokenException] must be thrownBy
          await(service.startSimulation(unknownToken, playRequest))

        requestMap must have size 1
        checkClientRequest(requestMap.head, ClientRequestStatus.SimulationNotFound)
      }
    }

    "simulationPing" should {
      "update ping value" in new WithSpecApplication {
        updateCalled must be (false)
        await(service.simulationPing(outcomeToken))
        updateCalled must be (true)
      }
    }

    "finishSimulation" should {
      "finish simulation with given outcome token" in new WithSpecApplication {
        val r = await(service.finishSimulation(outcomeToken, playRequest))

        checkClientRequest(requestMap.head, ClientRequestStatus.OutcomeFinished, None, Some(1))
        scoringOutcomes must have size 1
        expectScoringStarted()
      }

      "fail finish simulation with given outcome token which is already finished" in new WithSpecApplication {
        an [OutcomeAlreadyFinishedException] must be thrownBy
          await(service.finishSimulation(finishedOutcomeToken, playRequest))

        checkClientRequest(requestMap.head, ClientRequestStatus.OutcomeAlreadyFinished, None, Some(1))
        scoringOutcomes must be ('empty)
      }

      "fail finish simulation with missing token" in new WithSpecApplication {
        an [OutcomeNotFoundByTokenException] must be thrownBy
          await(service.finishSimulation(unknownToken, playRequest))

        checkClientRequest(requestMap.head, ClientRequestStatus.OutcomeNotFound)
        scoringOutcomes must be ('empty)
      }
    }

    "storeTestResult" should {
      "store single test result with given outcome token" in new WithSpecApplication {
        val request = TestResult(TaskId(1), "runner-01", result = true, Some("message-01"), Some(JsObject(Map("r" -> JsNumber(42)))), Some(1000))
        val r = await(service.storeTestResult(outcomeToken, TaskId(1), request, playRequest))
        checkClientRequest(requestMap.head, ClientRequestStatus.TestResult, None, Some(1))
        outcomes must have size 1
      }

      "fail store test result with given outcome token which is already finished" in new WithSpecApplication {
        val request = TestResult(TaskId(1), "runner-01", result = true, Some("message-01"), Some(JsObject(Map("r" -> JsNumber(42)))), Some(1000))

        an [OutcomeAlreadyFinishedException] must be thrownBy
          await(service.storeTestResult(finishedOutcomeToken, TaskId(1), request, playRequest))

        checkClientRequest(requestMap.head, ClientRequestStatus.OutcomeAlreadyFinished, None, Some(1))
        outcomes must be ('empty)
      }

      "fail store test result with missing token" in new WithSpecApplication {
        val request = TestResult(TaskId(1), "runner-01", result = true, Some("message-01"), Some(JsObject(Map("r" -> JsNumber(42)))), Some(1000))

        an [OutcomeNotFoundByTokenException] must be thrownBy
          await(service.storeTestResult(unknownToken, TaskId(1), request, playRequest))

        checkClientRequest(requestMap.head, ClientRequestStatus.OutcomeNotFound)
        outcomes must be ('empty)
      }

      "fail store test result with missing task" in new WithSpecApplication {
        val request = TestResult(TaskId(1), "runner-01", result = true, Some("message-01"), Some(JsObject(Map("r" -> JsNumber(42)))), Some(1000))

        an [TaskNotFoundByIdException] must be thrownBy
          await(service.storeTestResult(outcomeToken, TaskId(2), request, playRequest))

        checkClientRequest(requestMap.head, ClientRequestStatus.MissingTasks, None, Some(1))
        outcomes must be ('empty)
      }
    }
  }
}
