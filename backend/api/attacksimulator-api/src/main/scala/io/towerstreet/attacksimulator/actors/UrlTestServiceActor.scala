package io.towerstreet.attacksimulator.actors

import akka.actor.{Actor, Props}
import akka.pattern.pipe
import com.google.inject.Inject
import io.towerstreet.attacksimulator.services.UrlTestService
import io.towerstreet.attacksimulator.services.UrlTestService.UrlTestRecord
import io.towerstreet.logging.Logging

import scala.concurrent.ExecutionContext

object UrlTestServiceActor {
  def props(urlTestService: UrlTestService)(implicit ec: ExecutionContext): Props = Props(new UrlTestServiceActor(urlTestService))
}

class UrlTestServiceActor @Inject()(urlTestService: UrlTestService)(implicit ec: ExecutionContext) extends Actor with Logging {
  def receive: Receive = {
    case record: UrlTestRecord =>
      logger.debug(s"Storing records: $record")
      urlTestService.storeUrlTest(record) pipeTo sender()
  }
}
