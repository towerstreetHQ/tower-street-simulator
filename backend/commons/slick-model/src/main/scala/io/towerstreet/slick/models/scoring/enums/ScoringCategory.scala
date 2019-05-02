package io.towerstreet.slick.models.scoring.enums

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}
import enumeratum.EnumEntry.Hyphencase

sealed trait ScoringCategory extends EnumEntry with Hyphencase

object ScoringCategory extends Enum[ScoringCategory] with PlayJsonEnum[ScoringCategory] {

  val values = findValues

  case object Cis extends ScoringCategory
  case object Score extends ScoringCategory
}
