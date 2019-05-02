package io.towerstreet.attacksimulator.controllers

import java.util.UUID

import io.towerstreet.attacksimulator.models.Network.NetworkSegment
import io.towerstreet.attacksimulator.services.NetworkTestService
import io.towerstreet.controllers.ControllerHelpers
import io.towerstreet.slick.models.generated.attacksimulator.Model.TaskId
import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.ExecutionContext

@Singleton
class NetworkTestController @Inject()(cc: ControllerComponents,
                                      networkTestService: NetworkTestService
                                     )(implicit ec: ExecutionContext)
  extends AbstractController(cc)
    with ControllerHelpers
{

  def getClientSegments(token: UUID) =
    Action.async { implicit request =>
      networkTestService.getClientSegments(token, request)
        .toJson()
    }

  def postSegmentDiscovered(token: UUID, testId: Int) =
    Action(validateJson[NetworkSegment]).async { implicit request =>
      networkTestService.segmentDiscovered(token, TaskId(testId), request.body, request)
        .noContent()
    }
}
