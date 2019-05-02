package io.towerstreet.attacksimulator.testhelpers.helpers

import io.towerstreet.slick.models.attacksimulator.enums.ClientRequestStatus
import org.scalatestplus.play.PlaySpec

trait WithCheckClientRequest {
  this: PlaySpec =>

  lazy val userAgent = "my-test-agent"

  def checkClientRequest(req: Seq[Any],
                         status: ClientRequestStatus,
                         simulationId: Option[Int] = None,
                         outcomeId: Option[Int] = None,
                         receivedDataId: Option[Int] = None
                        ) = {
    req must have size 9
    req(0) mustBe "127.0.0.1"
    req(1) mustBe userAgent
    Option(req(2)) mustBe defined
    req(3) mustBe "/"
    req(4) mustBe status.entryName
    Option(req(5)) mustBe simulationId
    Option(req(6)) mustBe outcomeId
    Option(req(7)) mustBe receivedDataId
    Option(req(8)) mustBe defined
  }
}
