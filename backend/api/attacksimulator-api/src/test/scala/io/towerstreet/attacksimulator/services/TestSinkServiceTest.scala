package io.towerstreet.attacksimulator.services

import java.util.UUID

import acolyte.jdbc.{QueryResult, StatementHandler}
import akka.util.ByteString
import io.towerstreet.attacksimulator.exceptions.{MissingFileBodyException, OutcomeAlreadyFinishedException, OutcomeNotFoundByTokenException, TaskNotFoundByIdException}
import io.towerstreet.attacksimulator.models.TestSinks.FileSinkResponse
import io.towerstreet.attacksimulator.testhelpers.db.Results
import io.towerstreet.attacksimulator.testhelpers.helpers.WithCheckClientRequest
import io.towerstreet.slick.models.attacksimulator.enums.ClientRequestStatus
import io.towerstreet.slick.models.generated.attacksimulator.Model.TaskId
import io.towerstreet.testhelpers.WithApplicationSpec.WithService
import io.towerstreet.testhelpers.db.ExecutorHandlers.{Handler, Param, Query, ~}
import io.towerstreet.testhelpers.db.{ResultHelpers, WithMockedDbApplication}
import org.scalatestplus.play.PlaySpec
import play.api.libs.Files.SingletonTemporaryFileCreator
import play.api.mvc.RawBuffer
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}

import scala.collection.mutable

class TestSinkServiceTest  extends PlaySpec
  with FutureAwaits
  with DefaultAwaitTimeout
  with WithCheckClientRequest
{
  private lazy val outcomeToken = UUID.fromString("6d942879-fb00-4e2a-b6f8-96328c723be1")
  private lazy val finishedOutcomeToken = UUID.fromString("87f9c76a-c37a-47ff-a5fc-61212cd141b0")
  private lazy val unknownToken = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff")
  private lazy val contentType = "application/octet-stream"
  private lazy val fileName = "test-file"
  private lazy val content = ByteString("Hello this is my test content")

  class WithSpecApplication(override val config: Map[String, String] = Map.empty)
    extends WithMockedDbApplication
      with WithService[TestSinkService]
  {
    def playRequest = FakeRequest()
      .withHeaders("User-Agent" -> userAgent)
      .withHeaders("Content-Type" -> contentType)
      .withBody(RawBuffer(content.size, SingletonTemporaryFileCreator, content))

    // To check if inserted request object is ok
    val requestMap = mutable.ArrayBuffer.empty[Seq[Any]]

    // To check if inserted outcome results objects are ok
    val data = mutable.ArrayBuffer.empty[Seq[Any]]

    override lazy val handler: StatementHandler = Handler("^select ") {
      case ~(Query("""^select .* from "attacksimulator"."simulation_outcome".* where "token" = '6d942879""")) => Results.singleSimulationOutcome(outcomeToken)
      case ~(Query("""^select .* from "attacksimulator"."simulation_outcome".* where "token" = '87f9c76a""")) => Results.singleSimulationOutcome(finishedOutcomeToken, true)
      case ~(Query("""^select .* from "attacksimulator"."simulation_outcome".* where "token"""")) => QueryResult.Nil

      case ~(Query("""^select .* from "attacksimulator"."task" where "id" = 1""")) => Results.singleTask
      case ~(Query("""^select .* from "attacksimulator"."task"""")) => QueryResult.Nil

      case ~(Param("""^insert into "attacksimulator"."received_data""""), v@_*) =>
        data += v
        ResultHelpers.intKeyResult(1)

      case ~(Param("""^insert into "attacksimulator"."client_request""""), v@_*) =>
        requestMap += v
        ResultHelpers.intKeyResult(1)
    }
  }

  "TestSinkServiceTest" should {

    "fileSink" should {
      "process received file" in new WithSpecApplication {
        val r = await(service.fileSink(fileName, outcomeToken, TaskId(1), playRequest))

        r mustBe FileSinkResponse(fileName, Some(contentType), content.size)
        checkClientRequest(requestMap.head, ClientRequestStatus.FileDataReceived, outcomeId = Some(1), receivedDataId = Some(1))

        data must have size 1
      }

      "fail process received file for already finished outcome" in new WithSpecApplication {
        an [OutcomeAlreadyFinishedException] must be thrownBy
          await(service.fileSink(fileName, finishedOutcomeToken, TaskId(1), playRequest))

        checkClientRequest(requestMap.head, ClientRequestStatus.OutcomeAlreadyFinished, outcomeId = Some(1))
        data must be ('empty)
      }

      "fail process received file for missing outcome" in new WithSpecApplication {
        an [OutcomeNotFoundByTokenException] must be thrownBy
          await(service.fileSink(fileName, unknownToken, TaskId(1), playRequest))

        checkClientRequest(requestMap.head, ClientRequestStatus.OutcomeNotFound)
        data must be ('empty)
      }

      "fail process received file for missing task" in new WithSpecApplication {
        an [TaskNotFoundByIdException] must be thrownBy
          await(service.fileSink(fileName, outcomeToken, TaskId(2), playRequest))

        checkClientRequest(requestMap.head, ClientRequestStatus.MissingTasks, outcomeId = Some(1))
        data must be ('empty)
      }
    }

  }
}
