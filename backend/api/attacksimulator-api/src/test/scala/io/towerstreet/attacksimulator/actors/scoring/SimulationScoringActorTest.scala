package io.towerstreet.attacksimulator.actors.scoring

import java.time.LocalDateTime

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestKit}
import io.towerstreet.attacksimulator.models.Scoring.ScoringDefinitionsWithOutcome
import io.towerstreet.attacksimulator.services.scoring.ScoringService
import io.towerstreet.slick.models.generated.attacksimulator.Model.{SimulationOutcomeId, SimulationTemplateId}
import io.towerstreet.slick.models.generated.public.Model.CustomerAssessmentId
import io.towerstreet.slick.models.generated.scoring.Model._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

import scala.concurrent.{ExecutionContext, Future}

class SimulationScoringActorTest
  extends TestKit(ActorSystem("testSystem"))
    with ImplicitSender
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with FutureAwaits
    with DefaultAwaitTimeout
{
  implicit val ec: ExecutionContext = ExecutionContext.global

  private lazy val simulationDate = LocalDateTime.now()

  private def getOutcomeForScoring(id: Int) = {
    SimulationOutcomeForScoring(SimulationOutcomeId(id), simulationDate, Some(simulationDate),
      SimulationTemplateId(id), customerAssessmentId = CustomerAssessmentId(id))
  }

  class MockedScoringService(loadedSimulationOutcomesForScoring: Seq[SimulationOutcomeForScoring] = Seq.empty,
                             isSuccess: Boolean = true
                            ) extends ScoringService(null, null, null, null, null)(null) {
    override def performScoring(simulationOutcome: SimulationOutcomeForScoring): Future[ScoringDefinitionsWithOutcome] = {
      if (isSuccess) {
        Future.successful(ScoringDefinitionsWithOutcome(
          ScoringOutcome(ScoringOutcomeId(1), CustomerAssessmentId(1), Some(SimulationOutcomeId(1)), LocalDateTime.now(), None),
          Seq(),
          Seq(),
          None
        ))
      } else {
        Future.failed(new RuntimeException("Arbitrary test exception"))
      }
    }
  }

  "SimulationScoringActor" should {
    "perform scoring and send back end message" in {
      val outcome = getOutcomeForScoring(1)
      val actor = system.actorOf(SimulationScoringActor.props(new MockedScoringService, outcome))

      await(actor ? SimulationScoringActor.PerformScoring) should be (ScoringActor.ScoringEnded(outcome.simulationOutcomeId))
    }

    "send back end message even if scoring crashes" in {
      val outcome = getOutcomeForScoring(1)
      val actor = system.actorOf(SimulationScoringActor.props(new MockedScoringService(isSuccess = false), outcome))

      await(actor ? SimulationScoringActor.PerformScoring) should be (ScoringActor.ScoringEnded(outcome.simulationOutcomeId))
    }
  }
}
