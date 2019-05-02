package io.towerstreet.attacksimulator.services

import java.util.UUID

import acolyte.jdbc.{QueryResult, RowLists, StatementHandler}
import com.github.tminglei.slickpg.InetString
import io.towerstreet.attacksimulator.exceptions.{OutcomeAlreadyFinishedException, OutcomeNotFoundByTokenException, TaskNotFoundByIdException}
import io.towerstreet.attacksimulator.models.Network.NetworkSegment
import io.towerstreet.attacksimulator.testhelpers.db.Results
import io.towerstreet.attacksimulator.testhelpers.helpers.WithCheckClientRequest
import io.towerstreet.slick.models.attacksimulator.enums.ClientRequestStatus
import io.towerstreet.slick.models.generated.attacksimulator.Model
import io.towerstreet.slick.models.generated.attacksimulator.Model.{NetworkSegmentId, SimulationOutcomeId, TaskId}
import io.towerstreet.testhelpers.WithApplicationSpec.WithService
import io.towerstreet.testhelpers.db.ExecutorHandlers.{Handler, Param, Query, ~}
import io.towerstreet.testhelpers.db.{ResultHelpers, WithMockedDbApplication}
import org.scalatestplus.play.PlaySpec
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}

import scala.collection.mutable

class NetworkTestServiceTest
  extends PlaySpec
    with FutureAwaits
    with DefaultAwaitTimeout
    with WithCheckClientRequest
{

  private lazy val outcomeToken = UUID.fromString("6d942879-fb00-4e2a-b6f8-96328c723be1")
  private lazy val finishedOutcomeToken = UUID.fromString("87f9c76a-c37a-47ff-a5fc-61212cd141b0")
  private lazy val unknownToken = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff")

  class WithSpecApplication
    extends WithMockedDbApplication
      with WithService[NetworkTestService]
  {
    def playRequest = FakeRequest()
      .withHeaders("User-Agent" -> userAgent)

    // To check if inserted request object is ok
    val requestMap = mutable.ArrayBuffer.empty[Seq[Any]]

    val segments = mutable.ArrayBuffer.empty[Seq[Any]]

    override lazy val handler: StatementHandler = Handler("^select ") {
      case ~(Query("""^select .* from "attacksimulator"."simulation_outcome".* where "token" = '6d942879""")) => Results.singleSimulationOutcome(outcomeToken)
      case ~(Query("""^select .* from "attacksimulator"."simulation_outcome".* where "token" = '87f9c76a""")) => Results.singleSimulationOutcome(finishedOutcomeToken, true)
      case ~(Query("""^select .* from "attacksimulator"."simulation_outcome".* where "token"""")) => QueryResult.Nil

      case ~(Query("""^select .* from "attacksimulator"."task" where "id" = 1""")) => Results.singleTask
      case ~(Query("""^select .* from "attacksimulator"."task"""")) => QueryResult.Nil

      case ~(Query("""^select x2."customer_id" from "attacksimulator"."simulation_outcome"""")) => RowLists.intList(1).asResult

      case ~(Query("""^select x2."subnet_ip", x2."subnet_prefix" from "attacksimulator"."network_segment"""")) => Results.networkSegmentIp

      case ~(Param("""^insert into "attacksimulator"."client_request""""), v@_*) =>
        requestMap += v
        ResultHelpers.intKeyResult(1)

      case ~(Param("""^insert into "attacksimulator"."network_segment""""), v@_*) =>
        segments += v
        ResultHelpers.intKeyResult(1)
    }
  }

  "NetworkTestServiceTest" should {

    "segmentDiscovered" should {
      "insert new discovered network segment" in new WithSpecApplication {
        val segment = NetworkSegment("10.0.10.12", 24)
        val r = await(service.segmentDiscovered(outcomeToken, TaskId(1),segment , playRequest))

        r mustBe Model.NetworkSegment(NetworkSegmentId(1), SimulationOutcomeId(1), TaskId(1),
          InetString("10.0.10.12"),
          InetString("10.0.10.0"),
          24,
          r.createdAt
        )

        checkClientRequest(requestMap.head, ClientRequestStatus.NetworkSegmentDetected, outcomeId = Some(1))
        segments must have size 1
      }

      "fail insert new discovered network segment with given outcome token which is already finished" in new WithSpecApplication {
        val segment = NetworkSegment("10.0.10.12", 24)

        an [OutcomeAlreadyFinishedException] must be thrownBy
          await(service.segmentDiscovered(finishedOutcomeToken, TaskId(1),segment , playRequest))

        checkClientRequest(requestMap.head, ClientRequestStatus.OutcomeAlreadyFinished, None, Some(1))
        segments must be ('empty)
      }

      "fail insert new discovered network segment with missing token" in new WithSpecApplication {
        val segment = NetworkSegment("10.0.10.12", 24)

        an [OutcomeNotFoundByTokenException] must be thrownBy
          await(service.segmentDiscovered(unknownToken, TaskId(1),segment , playRequest))

        checkClientRequest(requestMap.head, ClientRequestStatus.OutcomeNotFound)
        segments must be ('empty)
      }

      "fail insert new discovered network segment with missing task" in new WithSpecApplication {
        val segment = NetworkSegment("10.0.10.12", 24)

        an [TaskNotFoundByIdException] must be thrownBy
          await(service.segmentDiscovered(outcomeToken, TaskId(2),segment , playRequest))

        checkClientRequest(requestMap.head, ClientRequestStatus.MissingTasks, None, Some(1))
        segments must be ('empty)
      }
    }

    "getClientSegments" should {
      "get discovered network segments" in new WithSpecApplication {
        val r = await(service.getClientSegments(outcomeToken, playRequest))
        r must have size 3

        r mustBe Seq(
          NetworkSegment("10.0.11.0", 26),
          NetworkSegment("10.0.12.0", 26),
          NetworkSegment("10.0.13.0", 26)
        )

        checkClientRequest(requestMap.head, ClientRequestStatus.NetworkSegmentsRetrieved, outcomeId = Some(1))
      }

      "fail get discovered network segments with given outcome token which is already finished" in new WithSpecApplication {
        an [OutcomeAlreadyFinishedException] must be thrownBy
          await(service.getClientSegments(finishedOutcomeToken, playRequest))

        checkClientRequest(requestMap.head, ClientRequestStatus.OutcomeAlreadyFinished, None, Some(1))
      }

      "fail get discovered network segments with missing token" in new WithSpecApplication {
        an [OutcomeNotFoundByTokenException] must be thrownBy
          await(service.getClientSegments(unknownToken, playRequest))

        checkClientRequest(requestMap.head, ClientRequestStatus.OutcomeNotFound)
      }
    }

  }
}
