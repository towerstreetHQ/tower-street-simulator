package io.towerstreet.slick.models.scoring.enums

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}
import enumeratum.EnumEntry.Hyphencase

sealed trait ScoringResultType extends EnumEntry with Hyphencase

object ScoringResultType extends Enum[ScoringResultType] with PlayJsonEnum[ScoringResultType] {

  val values = findValues

  case object NotRun extends ScoringResultType
  case object True extends ScoringResultType
  case object False extends ScoringResultType
  case object Unknown extends ScoringResultType
  case object Error extends ScoringResultType

  def apply(result: Boolean): ScoringResultType = {
    if (result) True
    else False
  }
}
