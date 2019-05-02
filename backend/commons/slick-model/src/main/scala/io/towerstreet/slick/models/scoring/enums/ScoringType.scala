package io.towerstreet.slick.models.scoring.enums

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}
import enumeratum.EnumEntry.Hyphencase

sealed trait ScoringType extends EnumEntry with Hyphencase

object ScoringType extends Enum[ScoringType] with PlayJsonEnum[ScoringType] {

  val values = findValues

  case object BooleanResult extends ScoringType
  case object UrlTestResult extends ScoringType
  case object OpenPortScanResult extends ScoringType
  case object FirewallPortScanResult extends ScoringType
  case object ExfiltrationResult extends ScoringType
}