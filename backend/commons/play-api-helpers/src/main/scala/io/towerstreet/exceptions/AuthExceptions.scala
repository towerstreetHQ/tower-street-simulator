package io.towerstreet.exceptions

import play.api.http.Status

object AuthExceptions {
  case object UnauthorizedException extends TowerStreetApiException {
    override val status = Status.UNAUTHORIZED
    val message = "Unauthorized"
  }

  case object InvalidCredentialsException extends TowerStreetApiException {
    val message = "Invalid username or password"
    override val status = Status.FORBIDDEN
  }
}
