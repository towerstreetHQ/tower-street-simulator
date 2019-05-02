package io.towerstreet.slick.models.attacksimulator.enums

import enumeratum._
import enumeratum.EnumEntry._

sealed trait ClientRequestStatus extends EnumEntry with Hyphencase

object ClientRequestStatus extends Enum[ClientRequestStatus] {

  val values = findValues

  case object SimulationStarted extends ClientRequestStatus
  case object SimulationNotFound extends ClientRequestStatus

  case object OutcomeFinished extends ClientRequestStatus
  case object TestResult extends ClientRequestStatus
  case object OutcomeAlreadyFinished extends ClientRequestStatus
  case object OutcomeNotFound extends ClientRequestStatus

  case object NetworkSegmentDetected extends ClientRequestStatus
  case object NetworkSegmentsRetrieved extends ClientRequestStatus

  case object FileDataReceived extends ClientRequestStatus
  case object MissingTasks extends ClientRequestStatus
  case object MissingFileContent extends ClientRequestStatus
}
