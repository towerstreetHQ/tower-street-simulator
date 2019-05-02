package io.towerstreet.attacksimulator.models

import io.towerstreet.json.{JsonValidators, TagFormat}
import io.towerstreet.slick.models.generated.attacksimulator.Model.Runner
import play.api.libs.json._

object Runners
  extends JsonValidators
    with TagFormat
{

  val RunnerNameMaxLength = 50
  val RunnerNameReader: Reads[String] = nonEmptyString(RunnerNameMaxLength)

  case class CreateRunnersRequest(runnerNames: Seq[String],
                                 )
  object CreateRunnersRequest {
    implicit val format: Format[CreateRunnersRequest] =
      Format(
        (JsPath \ "runnerNames").readSeq[String](RunnerNameReader)
          .map(CreateRunnersRequest(_)),
        Json.writes[CreateRunnersRequest]
      )
  }

  implicit val runnerFormat: Format[Runner] = Json.format[Runner]
}
