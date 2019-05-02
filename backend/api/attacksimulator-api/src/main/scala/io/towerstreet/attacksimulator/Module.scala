package io.towerstreet.attacksimulator

import com.google.inject.AbstractModule
import io.towerstreet.attacksimulator.actors.scoring.{ScoringActor, SimulationScoringActor}
import play.api.libs.concurrent.AkkaGuiceSupport

class Module extends AbstractModule with AkkaGuiceSupport {
  import actors._

  override def configure(): Unit = {
    bindActor[ScoringActor]("scoringActor")
    bindActorFactory[SimulationScoringActor, SimulationScoringActor.Factory]

    bindActor[UrlTestActor]("urlTestActor")
    bindActor[UrlTestServiceActor]("urlTestServiceActor")
  }
}
