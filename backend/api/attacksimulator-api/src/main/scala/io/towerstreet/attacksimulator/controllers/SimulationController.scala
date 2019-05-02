package io.towerstreet.attacksimulator.controllers

import java.util.UUID

import io.towerstreet.attacksimulator.models.Simulations.{FinishSimulationRequest, TestResult}
import io.towerstreet.attacksimulator.services.SimulationService
import io.towerstreet.controllers.ControllerHelpers
import io.towerstreet.slick.models.generated.attacksimulator.Model.TaskId
import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.ExecutionContext

@Singleton
class SimulationController @Inject()(cc: ControllerComponents,
                                     simulationService: SimulationService
                                    )(implicit ec: ExecutionContext)
  extends AbstractController(cc)
    with ControllerHelpers
{

  def postStartSimulation(token: UUID) =
    Action.async { implicit request =>
      simulationService
        .startSimulation(token, request)
        .toJson()
    }

  def postHeartbeat(token: UUID) =
    Action.async { implicit request =>
      simulationService
        .simulationPing(token)
        .noContent()
    }

  def postFinishSimulation(token: UUID) =
    Action.async { implicit request =>
      simulationService.finishSimulation(token, request)
        .noContent()
    }

  def postTestResult(token: UUID, testId: Int) =
    Action(validateJson[TestResult]).async { implicit request =>
      simulationService.storeTestResult(token, TaskId(testId), request.body, request)
        .noContent()
    }
}
