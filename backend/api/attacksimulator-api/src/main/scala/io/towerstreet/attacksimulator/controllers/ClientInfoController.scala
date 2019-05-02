package io.towerstreet.attacksimulator.controllers

import java.net.{Inet4Address, Inet6Address, InetAddress}

import io.towerstreet.controllers.ControllerHelpers
import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, Request}

import scala.concurrent.ExecutionContext
import scala.util.Try

@Singleton
class ClientInfoController @Inject()(cc: ControllerComponents)(implicit ec: ExecutionContext)
  extends AbstractController(cc)
  with ControllerHelpers
{
  /**
    * @return IPv4 address of X-Forwarded-For header if present (null otherwise). IPv6 IP address are deliberately
    *         ignored due to inability of port scanner to handle them.
    */
  def xForwardedFor() = Action { implicit request: Request[AnyContent] =>

    // Deployed version is currently behind loadbalanced proxy. X_FORWARDED_FOR header contains list of addresses
    // separated by colon. Try to get the first IP in the list to resolve client address after its local proxy.
    // TODO: Check behavior of this endpoint on AWS deploy
    jsonResponse(
      request.headers
        .get(X_FORWARDED_FOR)
        .flatMap(_.split(",").headOption)
        .flatMap(address => Try(InetAddress.getByName(address.trim)).toOption)
        .flatMap(_ match {
          case a: Inet4Address                              => Some(a.getHostAddress)
          case a: Inet6Address if a.isIPv4CompatibleAddress => Some(a.getHostAddress)
          case _                                            => None
        })
    )
  }

}
