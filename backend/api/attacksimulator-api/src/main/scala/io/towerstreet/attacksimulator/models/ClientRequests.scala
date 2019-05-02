package io.towerstreet.attacksimulator.models

import java.time.LocalDateTime
import java.util.UUID

import com.github.tminglei.slickpg.InetString
import io.towerstreet.services.helpers.PlayRequestHelpers
import io.towerstreet.slick.models.attacksimulator.enums.ClientRequestStatus
import io.towerstreet.slick.models.generated.attacksimulator.Model._
import play.api.mvc.Request

object ClientRequests {
  def createRequest(request: Request[_],
                    status: ClientRequestStatus,
                    token: Option[UUID] = None,
                    simulation: Option[Simulation] = None,
                    outcome: Option[SimulationOutcome] = None
                   ): ClientRequest =
    createRequestWithIds(request, status, token, simulation.map(_.id), outcome.map(_.id))

  def createRequestWithIds(request: Request[_],
                           status: ClientRequestStatus,
                           token: Option[UUID] = None,
                           simulationId: Option[SimulationId] = None,
                           outcomeId: Option[SimulationOutcomeId] = None
                          ) =
    ClientRequest(
      ClientRequestId(0),
      InetString(PlayRequestHelpers.getIpAddress(request)),
      request.headers.get("User-Agent"),
      LocalDateTime.now(),
      request.path,
      status,
      simulationId,
      outcomeId,
      token = token
    )

}
