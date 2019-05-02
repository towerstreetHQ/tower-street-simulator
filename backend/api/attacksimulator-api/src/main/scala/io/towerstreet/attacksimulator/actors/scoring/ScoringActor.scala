package io.towerstreet.attacksimulator.actors.scoring

import java.time.LocalDateTime

import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import com.typesafe.config.Config
import io.towerstreet.attacksimulator.services.scoring.ScoringService
import io.towerstreet.logging.Logging
import io.towerstreet.slick.models.generated.attacksimulator.Model.SimulationOutcomeId
import io.towerstreet.slick.models.generated.scoring.Model._
import javax.inject.Inject
import play.api.libs.concurrent.InjectedActorSupport

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

object ScoringActor {

  def props(config: Config,
            scoringService: ScoringService,
            simulationActorFactory: SimulationScoringActor.Factory)
           (implicit ec: ExecutionContext): Props =
    Props(new ScoringActor(config, scoringService, simulationActorFactory))

  case object RestartScoringFromDb

  case object CleanCalculations

  case class ScoringEnded(simulationOutcomeId: SimulationOutcomeId)

  case class ActorCalculation(data: SimulationOutcomeForScoring,
                              startedAt: LocalDateTime,
                              actor: ActorRef
                             )

  private[scoring] case object GetRunningCount
  private[scoring] case object GetQueuedCount
}

/**
  * Root actor for simulation scoring functionality - responsible for orchestrating scoring calculations per simulation
  * outcome.
  *
  * Calculation can be started:
  *   1) By sending instance of SimulationOutcomeForScoring as a message - intent to start scoring asynchronously
  *      right after finishing a simulation
  *   2) By sending RestartScoringFromDb message - checks DB for idle outcomes (calculation started long ago and wasn't
  *      finished); this allows scoring engine to repeat calculation and avoid data loss on system crash or restart.
  *
  * Single scoring is performed within [[SimulationScoringActor]]. This actor creates child actors per each simulation
  * outcome. Only configurable number of actors can be running at a time. Other simulation outcomes are being queued.
  */
class ScoringActor @Inject()(config: Config,
                             scoringService: ScoringService,
                             simulationActorFactory: SimulationScoringActor.Factory
                            )
                            (implicit ec: ExecutionContext)
  extends Actor
    with Logging
    with InjectedActorSupport
{
  import ScoringActor._

  private val maxConcurrentScoring = config.getInt("io.towerstreet.attacksimulator.scoring.maxConcurrentScoring")
  private val calculationTimeout = config.getDuration("io.towerstreet.attacksimulator.scoring.calculationTimeout")

  private val queuedSimulationOutcomes = mutable.Set.empty[SimulationOutcomeId]
  private val simulationOutcomeQueue = mutable.Queue.empty[SimulationOutcomeForScoring]
  private val runningScoring = mutable.Map.empty[SimulationOutcomeId, ActorCalculation]

  override def receive: Receive = {
    // Try restart scoring for DB instances
    case RestartScoringFromDb =>
      pullScoring()

    // Check whether some actor is not working too long
    case CleanCalculations =>
      cleanCalculations()

    // Start single scoring
    case s: SimulationOutcomeForScoring =>
      processSimulationOutcome(s)

    // Message from child actor that calculation ended (schedules next item in queue)
    case ScoringEnded(simulationOutcomeId) =>
      scoringFinished(simulationOutcomeId)

    // Messages for testing
    case GetRunningCount => sender ! runningScoring.size
    case GetQueuedCount => sender ! queuedSimulationOutcomes.size
  }

  private def pullScoring() = {
    logger.debug(s"Pulling scoring from database")

    scoringService.loadSimulationOutcomesForScoring().andThen {
      case Success(outcomes) if outcomes.nonEmpty =>
        logger.info(s"Pulled ${outcomes.size} simulation outcomes to score")
        outcomes.foreach(o => self ! o)

      case Success(_) =>
        logger.debug(s"Nothing for scoring")

      case Failure(e) =>
        logger.error(s"Error while pulling simulation outcomes ${e.getMessage}", e)
    }
  }

  private def processSimulationOutcome(s: SimulationOutcomeForScoring) = {
    val alreadyProcessed =
      queuedSimulationOutcomes.contains(s.simulationOutcomeId) ||
        runningScoring.contains(s.simulationOutcomeId)

    if (!alreadyProcessed) {
      if (runningScoring.size < maxConcurrentScoring) {
        startSingleScoring(s)
      } else {
        logger.debug(s"Exceeded max number of running scoring calculations for simulation outcome: ${s.simulationOutcomeId}, scoring: ${s.scoringOutcomeId}, " +
          s"runningScoring: ${runningScoring.size}, maxConcurrentScoring: $maxConcurrentScoring, queueSize: ${simulationOutcomeQueue.size}")

        simulationOutcomeQueue.enqueue(s)
        queuedSimulationOutcomes += s.simulationOutcomeId
      }
    } else {
      logger.warn(s"Simulation outcome has been already enqueued for scoring: ${s.scoringOutcomeId}, scoring: ${s.scoringOutcomeId}, " +
        s"runningScoring: ${runningScoring.size}, maxConcurrentScoring: $maxConcurrentScoring, queueSize: ${simulationOutcomeQueue.size}")
    }
  }

  private def scoringFinished(simulationOutcomeId: SimulationOutcomeId) = {
    runningScoring.remove(simulationOutcomeId)
    startQueued()
  }

  private def startQueued() = {
    if (simulationOutcomeQueue.nonEmpty) {
      val outcome = simulationOutcomeQueue.dequeue()
      queuedSimulationOutcomes.remove(outcome.simulationOutcomeId)

      startSingleScoring(outcome)
    }
  }

  private def startSingleScoring(data: SimulationOutcomeForScoring) = {
    try {
      val actor = injectedChild(simulationActorFactory(data), s"simulation-scoring-actor:${data.simulationOutcomeId}")
      runningScoring.put(data.simulationOutcomeId, ActorCalculation(data, LocalDateTime.now(), actor))

      actor ! SimulationScoringActor.PerformScoring
    } catch {
      case e: Throwable =>
        // Error while creating actor or sending message
        // Error can be created because the naming conflicts, leave to be restarted from DB
        logger.error(s"Error while starting scoring for simulation outcome ${data.simulationOutcomeId} ${e.getMessage}", e)

        runningScoring.remove(data.simulationOutcomeId)
    }
  }

  /**
    * Kills all child actors which exceeded calculation timeout - sends poison pill to them and releases runningScoring
    * so new actor can be created. Released scoring will be scheduled for recalculation from DB.
    *
    * There can still be problem by sending poison pill because actor can be already inactive. In that case actor will
    * be removed from runningScoring so new actor can be spawned.
    */
  private def cleanCalculations() = {
    val threshold = LocalDateTime.now().minus(calculationTimeout)

    runningScoring
      .filter(_._2.startedAt.isBefore(threshold))
      .foreach {
        case (id, ActorCalculation(data, _, actor)) =>
          logger.warn(s"Killing long running actor after exceeding $calculationTimeout for simulation " +
            s"outcome: ${data.simulationOutcomeId}")

          runningScoring.remove(id)
          actor ! PoisonPill
          startQueued()
      }
  }
}