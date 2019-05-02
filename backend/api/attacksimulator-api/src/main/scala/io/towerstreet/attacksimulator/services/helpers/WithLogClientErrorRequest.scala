package io.towerstreet.attacksimulator.services.helpers

import java.util.UUID

import io.towerstreet.attacksimulator.dao.ClientRequestDAO
import io.towerstreet.attacksimulator.models.ClientRequests
import io.towerstreet.slick.models.attacksimulator.enums.ClientRequestStatus
import io.towerstreet.slick.models.generated.attacksimulator.Model.{SimulationId, SimulationOutcomeId}
import play.api.mvc.Request

import scala.concurrent.{ExecutionContext, Future}

trait WithLogClientErrorRequest {
  val clientRequestDAO: ClientRequestDAO

  protected def logErrorRequest(error: Throwable,
                              request: Request[_],
                              status: ClientRequestStatus,
                              token: Option[UUID] = None,
                              simulationId: Option[SimulationId] = None,
                              outcomeId: Option[SimulationOutcomeId] = None
                             )(implicit executionContext: ExecutionContext) = {
    val errorRequest = ClientRequests.createRequestWithIds(request, status, token, simulationId, outcomeId)

    clientRequestDAO
      .insert(errorRequest)
      .flatMap(_ => Future.failed(error))
  }
}
