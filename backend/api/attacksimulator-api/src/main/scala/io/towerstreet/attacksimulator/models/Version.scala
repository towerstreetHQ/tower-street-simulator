package io.towerstreet.attacksimulator.models

import io.towerstreet.attacksimulator.BuildInfo
import play.api.libs.json._

case class Version(name: String,
                   version: String,
                   scalaVersion: String,
                   sbtVersion: String
                  )

object Version {

  lazy val BuildInfoVersion =
    Version(
      BuildInfo.name,
      BuildInfo.version,
      BuildInfo.scalaVersion,
      BuildInfo.sbtVersion
    )

  implicit val writes: Writes[Version] = Json.writes[Version]
}
