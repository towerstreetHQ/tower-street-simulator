package io.towerstreet.attacksimulator.scheduler

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem}
import com.typesafe.config.Config
import io.towerstreet.attacksimulator.actors.scoring.ScoringActor.{CleanCalculations, RestartScoringFromDb}
import io.towerstreet.logging.Logging
import javax.inject.{Inject, Named}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class ScoringScheduler @Inject()(actorSystem: ActorSystem,
                                 config: Config,
                                 @Named("scoringActor") scoringActor: ActorRef
                                )(implicit executionContext: ExecutionContext)
  extends Logging
{
  private val restartEnabled = config.getBoolean("io.towerstreet.attacksimulator.scoring.restartEnabled")
  private val restartInterval = FiniteDuration(config.getDuration("io.towerstreet.attacksimulator.scoring.restartInterval").getSeconds, TimeUnit.SECONDS)
  private val cleanCalculationInterval = FiniteDuration(config.getDuration("io.towerstreet.attacksimulator.scoring.cleanCalculationInterval").getSeconds, TimeUnit.SECONDS)

  if (restartEnabled) {
    logger.info(s"Enabling scheduler to restart simulation outcome scoring every $restartInterval")
    actorSystem.scheduler.schedule(initialDelay = 0.microseconds, interval = restartInterval, scoringActor, RestartScoringFromDb)
  } else {
    logger.warn(s"Scheduler to restart simulation outcome scoring is disabled")
  }

  logger.info(s"Enabling scheduler to clean timeout calculations every $cleanCalculationInterval")
  actorSystem.scheduler.schedule(initialDelay = 0.microseconds, interval = cleanCalculationInterval, scoringActor, CleanCalculations)
}
