package io.towerstreet.attacksimulator.models

import java.util.UUID

import com.github.vitalsoftware.macros.{json, jsonDefaults}
import io.towerstreet.attacksimulator.helpers.JsonHelpers
import io.towerstreet.attacksimulator.models.Tasks.TaskKeyWithParameters
import io.towerstreet.json.{JsonValidators, TagFormat}
import io.towerstreet.slick.models.generated.attacksimulator.Model._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.Reads._

object Simulations
  extends JsonValidators
    with TagFormat
    with JsonHelpers
{
  val SimulationDescriptionMax = 50

  /* ***************
   * Creating simulations
   */

  case class CreateSimulationRequest(taskKeys: Seq[String] = Seq(),
                                     tasksWithParameters: Seq[TaskKeyWithParameters] = Seq(),
                                     description: Option[String] = None)
  object CreateSimulationRequest {
    implicit val format: Format[CreateSimulationRequest] =
      Format(
        ((JsPath \ "taskKeys").readSeqOpt[String](Tasks.TaskKeyReader) and
          (JsPath \ "tasksWithParameters").readSeqOpt[TaskKeyWithParameters] and
          (JsPath \ "description").readNullable[String](maxLength(SimulationDescriptionMax))
          )(CreateSimulationRequest.apply _),
        Json.writes[CreateSimulationRequest]
      )
  }

  @jsonDefaults
  case class CreateBatchedSimulationsRequest(definition: CreateSimulationRequest,
                                             includedUsers: Seq[UserId] = Seq.empty,
                                             excludedUsers: Seq[UserId] = Seq.empty
                                            )


  /* ***************
   * Returning simulations
   */

  @json
  case class SimulationResponse(id: SimulationId,
                                userId: UserId,
                                userName: String,
                                token: java.util.UUID,
                                description: Option[String] = None
                               )
  object SimulationResponse {
    def apply(simulation: Simulation, user: User): SimulationResponse =
      SimulationResponse(simulation.id, user.id, user.userName, simulation.token, simulation.description)

    def apply(t: (Simulation, User)): SimulationResponse =
      SimulationResponse(t._1, t._2)
  }


  /* ***************
   * Starting simulations
   */

  case class TestDefinitionResponse(testId: TaskId,
                                    testRunner: String,
                                    parameters: Option[JsValue],
                                    label: Option[String]
                                   )
  object TestDefinitionResponse {
    implicit val format: Format[TestDefinitionResponse] =
      Format(
        Json.reads[TestDefinitionResponse],
        ((JsPath \ "testId").write[TaskId] and
          (JsPath \ "testRunner").write[String] and
          JsPath.writeNullable[JsValue] and
          (JsPath \ "label").write[Option[String]]
        )(unlift(TestDefinitionResponse.unapply))
      )
  }

  @json
  case class StartSimulationResponse(outcomeToken: UUID,
                                     tests: Seq[TestDefinitionResponse]
                                    )
  object StartSimulationResponse {
    def apply(outcome: SimulationOutcome, tasks: Seq[TaskForSimulation]): StartSimulationResponse = {
      StartSimulationResponse(
        outcome.token,
        tasks.map { t =>
          TestDefinitionResponse(t.id, t.runnerName, t.parameters, t.labelInSimulation)
        }
      )
    }
  }

  /* ***************
   * Finishing simulations
   */

  case class TestResult(testId: TaskId,
                        testRunner: String,
                        result: Boolean,
                        message: Option[String],
                        obj: Option[JsValue],
                        duration: Option[Double] = None,
                       )
  object TestResult {
    implicit val format: Format[TestResult] =
      Format(
        ((JsPath \ "testId").read[TaskId] and
          (JsPath \ "testRunner").read[String](Runners.RunnerNameReader) and
          (JsPath \ "result").read[Boolean] and
          (JsPath \ "message").readNullable[String] and
          (JsPath \ "obj").readNullable[JsValue] and
          (JsPath \ "duration").readNullable[Double]
          )(TestResult.apply _),
        Json.writes[TestResult]
      )
  }

  case class FinishSimulationRequest(results: Seq[TestResult])
  object FinishSimulationRequest {
    implicit val format: Format[FinishSimulationRequest] = Json.format[FinishSimulationRequest]
  }
}
