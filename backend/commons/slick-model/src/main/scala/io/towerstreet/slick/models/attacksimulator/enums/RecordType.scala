package io.towerstreet.slick.models.attacksimulator.enums

import enumeratum.EnumEntry.Hyphencase
import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

sealed trait RecordType extends EnumEntry with Hyphencase
object RecordType extends Enum[RecordType] with PlayJsonEnum[RecordType] {

  val values = findValues

  case object Pci extends RecordType
  case object Ssn extends RecordType
}
