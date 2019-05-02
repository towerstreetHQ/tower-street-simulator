package io.towerstreet.slick.models.public.enums

import enumeratum.EnumEntry.Hyphencase
import enumeratum.{Enum, EnumEntry}

sealed trait UserTokenType extends EnumEntry with Hyphencase

object UserTokenType extends Enum[UserTokenType] {

  val values = findValues

  case object Activation extends UserTokenType
  case object ForgotPassword extends UserTokenType
}
