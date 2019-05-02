package io.towerstreet.services.helpers

import io.towerstreet.logging.Logging
import play.api.http.HeaderNames
import play.api.mvc.Request

object PlayRequestHelpers
  extends Logging
    with HeaderNames
{
  // Proxies are stacking IP addresses in array separated by colon. We are using two proxies - loadbalancer and
  // inner instance proxy so we need to look for IP address at the last but one position of X_FORWARDED_FOR
  private val xForwardedForIpSuffix = 2

  def getIpAddress(request: Request[_]): String = {
    request.headers.get(X_FORWARDED_FOR)
      .flatMap { xForwarded =>
        val split = xForwarded.split(",")
        if (split.length >= xForwardedForIpSuffix) {
          Some(split(split.length - xForwardedForIpSuffix).trim)
        } else {
          None
        }
      }.getOrElse(request.remoteAddress)
  }
}
