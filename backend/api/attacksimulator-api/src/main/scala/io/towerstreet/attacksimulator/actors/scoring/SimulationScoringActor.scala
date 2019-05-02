package io.towerstreet.attacksimulator.actors.scoring

import akka.actor.{Actor, Props}
import akka.pattern.pipe
import com.google.inject.assistedinject.Assisted
import io.towerstreet.attacksimulator.services.scoring.ScoringService
import io.towerstreet.logging.Logging
import io.towerstreet.slick.models.generated.scoring.Model._
import javax.inject.Inject
import play.api.libs.concurrent.InjectedActorSupport

import scala.concurrent.ExecutionContext

object SimulationScoringActor {
  trait Factory {
    def apply(simulationOutcome: SimulationOutcomeForScoring): Actor
  }

  def props(scoringService: ScoringService,
            simulationOutcome: SimulationOutcomeForScoring)
           (implicit ec: ExecutionContext): Props =
    Props(new SimulationScoringActor(scoringService, simulationOutcome))

  case object PerformScoring
}

/**
  * Actor which represents scoring of single simulation outcome. After receiving message runs service method for
  * scoring and sends message back to parent that calculation finished.
  *
  * On error in future logs error message and let scoring framework to restart from DB (after timeout).
  */
class SimulationScoringActor @Inject()(scoringService: ScoringService,
                                       @Assisted simulationOutcome: SimulationOutcomeForScoring
                                      )(implicit ec: ExecutionContext)
  extends Actor
    with Logging
    with InjectedActorSupport
{
  import SimulationScoringActor._

  override def receive: Receive = {
    case PerformScoring =>
      scoringService.performScoring(simulationOutcome)
        .map { r =>
          logger.info(s"Finished scoring of simulation outcome: ${simulationOutcome.simulationOutcomeId}, " +
            s"scoring: ${r.scoringOutcome.id}")

          context.stop(self)
          ScoringActor.ScoringEnded(simulationOutcome.simulationOutcomeId)
        } .recover {
          case e: Throwable =>
            logger.error(s"Error while performing scoring for simulation outcome " +
              s"${simulationOutcome.simulationOutcomeId}, error:  ${e.getMessage}", e)

            context.stop(self)
            ScoringActor.ScoringEnded(simulationOutcome.simulationOutcomeId)
        } pipeTo sender
  }
}


