package io.towerstreet.attacksimulator.models

import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.Reads._

object Network {

  lazy val ipPattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$".r
  lazy val ipReader = pattern(ipPattern,"error.ipAddress.pattern")

  case class NetworkSegment(ip: String, prefix: Int)
  object NetworkSegment {
    implicit val format: Format[NetworkSegment] =
      Format(
        ((JsPath \ "ip").read[String](ipReader) and
          (JsPath \ "prefix").read[Int]
          )(NetworkSegment.apply _),
        Json.writes[NetworkSegment]
      )
  }
}
