package io.towerstreet.attacksimulator.services

import java.time.LocalDateTime
import java.util.UUID

import akka.util.ByteString
import com.typesafe.config.Config
import io.towerstreet.attacksimulator.dao.{ClientRequestDAO, ReceivedDataDAO, SimulationOutcomeDAO, TaskDAO}
import io.towerstreet.attacksimulator.exceptions.MissingFileBodyException
import io.towerstreet.attacksimulator.models.ClientRequests
import io.towerstreet.attacksimulator.models.TestSinks.FileSinkResponse
import io.towerstreet.attacksimulator.services.helpers.{WithGetOutcome, WithLogClientErrorRequest}
import io.towerstreet.logging.Logging
import io.towerstreet.slick.models.attacksimulator.enums.ClientRequestStatus
import io.towerstreet.slick.models.generated.attacksimulator.Model.{ReceivedData, ReceivedDataId, TaskId}
import javax.inject.{Inject, Singleton}
import play.api.mvc.{RawBuffer, Request}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TestSinkService @Inject()(config: Config,
                                val simulationOutcomeDAO: SimulationOutcomeDAO,
                                val taskDAO: TaskDAO,
                                receivedDataDAO: ReceivedDataDAO,
                                val clientRequestDAO: ClientRequestDAO
                               )(implicit ec: ExecutionContext)
  extends Logging
    with WithLogClientErrorRequest
    with WithGetOutcome
{
  private val maxFileSize = config.getBytes("io.towerstreet.attacksimulator.test.sinks.maxFileSize")

  def fileSink(fileName: String,
               outcomeToken: UUID,
               taskId: TaskId,
               request: Request[RawBuffer]
              ): Future[FileSinkResponse] = {

    val fileSize = request.body.size

    logger.info(s"Received data in test data sink, name: $fileName, token: $outcomeToken, " +
      s"dataSize: $fileSize, contentType: ${request.contentType}, charset: ${request.charset}")

    for {
      (outcome, task) <- getOutcomeAndTask(outcomeToken, taskId, request, "Error processing file file ")

      // Prepare data to be stored
      data = ReceivedData(ReceivedDataId(0), outcome.id, task.id, request.contentType, fileName,
        LocalDateTime.now(), fileSize)

      // Request logging
      clientRequest = ClientRequests.createRequest(request, ClientRequestStatus.FileDataReceived,
        Some(outcomeToken), outcome = Some(outcome))

      _ <- receivedDataDAO.insertWithRequest(data, clientRequest)
    } yield FileSinkResponse(fileName, data.contentType, data.size)
  }
}
