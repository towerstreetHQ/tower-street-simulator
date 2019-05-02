package io.towerstreet.attacksimulator.services

import java.time.LocalDateTime
import java.util.UUID

import com.github.tminglei.slickpg.InetString
import com.typesafe.config.Config
import io.towerstreet.attacksimulator.dao._
import io.towerstreet.attacksimulator.models.ClientRequests
import io.towerstreet.attacksimulator.models.Network.NetworkSegment
import io.towerstreet.attacksimulator.services.helpers.{WithGetOutcome, WithLogClientErrorRequest}
import io.towerstreet.logging.Logging
import io.towerstreet.slick.models.attacksimulator.enums.ClientRequestStatus
import io.towerstreet.slick.models.generated.attacksimulator.Model
import io.towerstreet.slick.models.generated.attacksimulator.Model.{NetworkSegmentId, TaskId}
import javax.inject.{Inject, Singleton}
import org.apache.commons.net.util.SubnetUtils
import play.api.mvc.Request

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NetworkTestService @Inject()(config: Config,
                                   val simulationOutcomeDAO: SimulationOutcomeDAO,
                                   val taskDAO: TaskDAO,
                                   val clientRequestDAO: ClientRequestDAO,
                                   networkSegmentDAO: NetworkSegmentDAO
                                  )(implicit ec: ExecutionContext)
  extends Logging
    with WithLogClientErrorRequest
    with WithGetOutcome
{
  def getClientSegments(outcomeToken: UUID,
                        request: Request[_]): Future[Seq[NetworkSegment]] = {
    logger.info(s"Retrieving client network segments for token: $outcomeToken")

    for {
      outcome <- getOutcome(outcomeToken, request, "Error retrieving network segments ")

      clientRequest = ClientRequests.createRequest(request, ClientRequestStatus.NetworkSegmentsRetrieved,
        Some(outcomeToken), outcome = Some(outcome))

      segments <- networkSegmentDAO.findClientSegments(outcome.id, clientRequest)
    } yield segments.map {
      case (ip, prefix) => NetworkSegment(ip.address, prefix)
    }
  }

  def segmentDiscovered(outcomeToken: UUID,
                        taskId: TaskId,
                        data: NetworkSegment,
                        request: Request[_]
                       ): Future[Model.NetworkSegment] = {
    logger.info(s"Received discovered network segment, token: $outcomeToken, taskId: $taskId, segment: $data")

    // Extract first address in subnet using apache commons. This is address normalization - network
    // address and prefix defines segment, two segments with same values are considered equal.
    val subnet = new SubnetUtils(s"${data.ip}/${data.prefix}").getInfo
    val networkAddress = subnet.getNetworkAddress

    for {
      (outcome, task) <- getOutcomeAndTask(outcomeToken, taskId, request, "Error discovering network segments")

      segment = Model.NetworkSegment(
        NetworkSegmentId(0), outcome.id, task.id,
        InetString(data.ip),
        InetString(networkAddress),
        data.prefix,
        LocalDateTime.now
      )

      clientRequest = ClientRequests.createRequest(request, ClientRequestStatus.NetworkSegmentDetected,
        Some(outcomeToken), outcome = Some(outcome))

      inserted <- networkSegmentDAO.insertWithRequest(segment, clientRequest)
    } yield inserted
  }
}
