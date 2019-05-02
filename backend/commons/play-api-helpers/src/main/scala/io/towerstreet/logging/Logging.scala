package io.towerstreet.logging

import play.api.Logger

trait Logging {
  val logger = Logger(this.getClass)
}
