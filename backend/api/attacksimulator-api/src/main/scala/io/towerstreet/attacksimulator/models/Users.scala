package io.towerstreet.attacksimulator.models

import com.github.vitalsoftware.macros.json
import io.towerstreet.attacksimulator.models.Simulations.{CreateSimulationRequest, SimulationResponse}
import io.towerstreet.json.{JsonValidators, TagFormat}
import io.towerstreet.slick.models.generated.attacksimulator.Model.{Simulation, User, UserId}
import play.api.libs.functional.syntax._
import play.api.libs.json._

object Users
  extends JsonValidators
    with TagFormat
{

  case class CreateUserRequest(userName: String,
                               simulations: Seq[CreateSimulationRequest] = Seq())
  object CreateUserRequest {
    implicit val format: Format[CreateUserRequest] =
      Format(
        ((JsPath \ "userName").read[String](nonEmptyString) and
          (JsPath \ "simulations").readSeqOpt[CreateSimulationRequest]
          )(CreateUserRequest.apply _),
        Json.writes[CreateUserRequest]
      )
  }

  case class UserWithSimulations(user: User, simulations: Seq[Simulation])

  @json
  case class CreateUserResponse(id: UserId,
                                userName: String,
                                simulations: Seq[SimulationResponse]
                               )
  object CreateUserResponse {
    def apply(user: User, simulations: Seq[Simulation]): CreateUserResponse =
      CreateUserResponse(user.id, user.userName, simulations.map(SimulationResponse(_, user)))
  }
}
