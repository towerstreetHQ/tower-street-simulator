package io.towerstreet.attacksimulator.scheduler

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.pipe
import io.towerstreet.attacksimulator.actors.UrlTestActor.TestTasks
import io.towerstreet.attacksimulator.constants.UrlTestConstants
import io.towerstreet.attacksimulator.dao.TaskDAO
import io.towerstreet.logging.Logging
import javax.inject.{Inject, Named}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class UrlTestScheduler @Inject()(actorSystem: ActorSystem,
                                  @Named("urlTestActor") urlTestActor: ActorRef,
                                  taskDAO: TaskDAO)
                                (implicit executionContext: ExecutionContext) extends Logging{

  actorSystem.scheduler.schedule(initialDelay = 0.microseconds, interval = 1.hour) {
    logger.info("Performing scheduled task UrlTestScheduler")
    taskDAO.getTasksByRunners(UrlTestConstants.UrlTestTasks)
      .map(TestTasks) pipeTo urlTestActor
  }

}

