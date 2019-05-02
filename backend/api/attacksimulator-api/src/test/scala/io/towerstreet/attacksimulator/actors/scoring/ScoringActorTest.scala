package io.towerstreet.attacksimulator.actors.scoring

import java.time
import java.time.LocalDateTime

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill}
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import io.towerstreet.attacksimulator.actors.scoring.ScoringActor.ScoringEnded
import io.towerstreet.attacksimulator.services.scoring.ScoringService
import io.towerstreet.slick.models.generated.attacksimulator.Model.{SimulationOutcomeId, SimulationTemplateId}
import io.towerstreet.slick.models.generated.public.Model.CustomerAssessmentId
import io.towerstreet.slick.models.generated.scoring.Model.SimulationOutcomeForScoring
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}


class ScoringActorTest
  extends TestKit(ActorSystem("testSystem"))
    with ImplicitSender
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with FutureAwaits
    with DefaultAwaitTimeout
{
  implicit val ec: ExecutionContext = ExecutionContext.global


  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  private lazy val simulationDate = LocalDateTime.now()

  private def getOutcomeForScoring(id: Int) = {
    SimulationOutcomeForScoring(SimulationOutcomeId(id), simulationDate, Some(simulationDate),
      SimulationTemplateId(id), customerAssessmentId = CustomerAssessmentId(id))
  }

  private lazy val outcomesForScoring = Seq(
    getOutcomeForScoring(1),
    getOutcomeForScoring(2)
  )

  class MockedScoringService(loadedSimulationOutcomesForScoring: Seq[SimulationOutcomeForScoring] = Seq.empty,
                             isSuccess: Boolean = true
                            ) extends ScoringService(null, null, null, null, null)(null) {
    override def loadSimulationOutcomesForScoring(): Future[Seq[SimulationOutcomeForScoring]] = {
      if (isSuccess) Future.successful(loadedSimulationOutcomesForScoring)
      else Future.failed(new RuntimeException("Arbitrary test exception"))
    }
  }

  class WithSpecActor {
    lazy val childProbe = TestProbe()
    lazy val config = ConfigFactory.load()

    val startedScoring = mutable.ArrayBuffer.empty[SimulationOutcomeForScoring]

    class ParentProbeActor(probe: ActorRef) extends Actor {
      def receive = {
        case e =>
          probe ! e
          context.stop(self)
      }
    }

    lazy val factory = new SimulationScoringActor.Factory {
      override def apply(simulationOutcome: SimulationOutcomeForScoring): Actor = {
        startedScoring += simulationOutcome
        new ParentProbeActor(childProbe.ref)
      }
    }
  }

  "ScoringActor" should {
    "SimulationOutcomeForScoring message" should {
      "start single calculation" in new WithSpecActor {
        private lazy val actor = system.actorOf(ScoringActor.props(config, new MockedScoringService, factory))
        private val message = getOutcomeForScoring(1)

        actor ! message

        childProbe.expectMsg(SimulationScoringActor.PerformScoring)

        await(actor ? ScoringActor.GetRunningCount) should be (1)
        await(actor ? ScoringActor.GetQueuedCount) should be (0)

        startedScoring should have size 1
        startedScoring should be (Seq(message))

        actor ! PoisonPill
      }

      "enqueue second config if limit is set to 1" in new WithSpecActor {
        private val overriddenConfig = config.withValue("io.towerstreet.attacksimulator.scoring.maxConcurrentScoring", ConfigValueFactory.fromAnyRef(1))

        private lazy val actor = system.actorOf(ScoringActor.props(overriddenConfig, new MockedScoringService, factory))
        private val message = getOutcomeForScoring(1)

        actor ! message
        childProbe.expectMsg(SimulationScoringActor.PerformScoring)

        actor ! getOutcomeForScoring(2)

        await(actor ? ScoringActor.GetRunningCount) should be (1)
        await(actor ? ScoringActor.GetQueuedCount) should be (1)

        startedScoring should have size 1
        startedScoring should be (Seq(message))

        actor ! PoisonPill
      }

      "do nothing if outcome is being processed" in new WithSpecActor {
        private lazy val actor = system.actorOf(ScoringActor.props(config, new MockedScoringService, factory))
        private val message = getOutcomeForScoring(1)

        actor ! message
        childProbe.expectMsg(SimulationScoringActor.PerformScoring)

        actor ! message

        await(actor ? ScoringActor.GetRunningCount) should be (1)
        await(actor ? ScoringActor.GetQueuedCount) should be (0)

        startedScoring should have size 1
        startedScoring should be (Seq(message))

        actor ! PoisonPill
      }

      "do nothing if outcome has been already queued" in new WithSpecActor {
        private val overriddenConfig = config.withValue("io.towerstreet.attacksimulator.scoring.maxConcurrentScoring", ConfigValueFactory.fromAnyRef(1))

        private lazy val actor = system.actorOf(ScoringActor.props(overriddenConfig, new MockedScoringService, factory))
        private val message = getOutcomeForScoring(1)

        actor ! message
        childProbe.expectMsg(SimulationScoringActor.PerformScoring)

        actor ! getOutcomeForScoring(2)
        actor ! getOutcomeForScoring(2)

        await(actor ? ScoringActor.GetRunningCount) should be (1)
        await(actor ? ScoringActor.GetQueuedCount) should be (1)

        startedScoring should have size 1
        startedScoring should be (Seq(message))

        actor ! PoisonPill
      }
    }

    "ScoringEnded message" should {
      "finish running scoring" in new WithSpecActor {
        private lazy val actor = system.actorOf(ScoringActor.props(config, new MockedScoringService, factory))
        private val message = getOutcomeForScoring(1)

        actor ! message
        childProbe.expectMsg(SimulationScoringActor.PerformScoring)

        await(actor ? ScoringActor.GetRunningCount) should be (1)
        await(actor ? ScoringActor.GetQueuedCount) should be (0)

        startedScoring should have size 1
        startedScoring should be (Seq(message))

        actor ! ScoringEnded(message.simulationOutcomeId)

        await(actor ? ScoringActor.GetRunningCount) should be (0)
        await(actor ? ScoringActor.GetQueuedCount) should be (0)

        actor ! PoisonPill
      }

      "do nothing if there is no such scoring" in new WithSpecActor {
        private lazy val actor = system.actorOf(ScoringActor.props(config, new MockedScoringService, factory))
        private val message = getOutcomeForScoring(1)

        actor ! ScoringEnded(message.simulationOutcomeId)

        await(actor ? ScoringActor.GetRunningCount) should be (0)
        await(actor ? ScoringActor.GetQueuedCount) should be (0)

        startedScoring should have size 0

        actor ! PoisonPill
      }

      "start enqueued scoring" in new WithSpecActor {
        private val overriddenConfig = config.withValue("io.towerstreet.attacksimulator.scoring.maxConcurrentScoring", ConfigValueFactory.fromAnyRef(1))

        private lazy val actor = system.actorOf(ScoringActor.props(overriddenConfig, new MockedScoringService, factory))
        private val message = getOutcomeForScoring(1)
        private val message2 = getOutcomeForScoring(2)

        actor ! message
        childProbe.expectMsg(SimulationScoringActor.PerformScoring)

        actor ! message2

        await(actor ? ScoringActor.GetRunningCount) should be (1)
        await(actor ? ScoringActor.GetQueuedCount) should be (1)

        startedScoring should have size 1
        startedScoring should be (Seq(message))

        actor ! ScoringEnded(message.simulationOutcomeId)
        childProbe.expectMsg(SimulationScoringActor.PerformScoring)

        startedScoring should have size 2
        startedScoring should be (Seq(message, message2))

        await(actor ? ScoringActor.GetRunningCount) should be (1)
        await(actor ? ScoringActor.GetQueuedCount) should be (0)

        actor ! PoisonPill
      }
    }

    "RestartScoringFromDb message" should {
      "pull simulation outcomes from DB and schedule scoring" in new WithSpecActor {
        private lazy val actor = system.actorOf(ScoringActor.props(config, new MockedScoringService(outcomesForScoring), factory))

        actor ! ScoringActor.RestartScoringFromDb

        childProbe.expectMsg(SimulationScoringActor.PerformScoring)
        childProbe.expectMsg(SimulationScoringActor.PerformScoring)

        startedScoring should have size 2
        startedScoring should be (outcomesForScoring)

        await(actor ? ScoringActor.GetRunningCount) should be (2)
        await(actor ? ScoringActor.GetQueuedCount) should be (0)

        actor ! PoisonPill
      }

      "do nothing if nothing is pulled" in new WithSpecActor {
        private lazy val actor = system.actorOf(ScoringActor.props(config, new MockedScoringService, factory))

        actor ! ScoringActor.RestartScoringFromDb

        await(actor ? ScoringActor.GetRunningCount) should be (0)
        await(actor ? ScoringActor.GetQueuedCount) should be (0)

        startedScoring should have size 0

        actor ! PoisonPill
      }

      "do nothing if pulling from DB crashed" in new WithSpecActor {
        private lazy val actor = system.actorOf(ScoringActor.props(config, new MockedScoringService(isSuccess = false), factory))

        actor ! ScoringActor.RestartScoringFromDb

        await(actor ? ScoringActor.GetRunningCount) should be (0)
        await(actor ? ScoringActor.GetQueuedCount) should be (0)

        startedScoring should have size 0

        actor ! PoisonPill
      }
    }

    "CleanCalculations message" should {
      "kill actor if calculation exceeded limit" in new WithSpecActor {
        private val overriddenConfig = config.withValue("io.towerstreet.attacksimulator.scoring.calculationTimeout", ConfigValueFactory.fromAnyRef(time.Duration.ofHours(-1)))

        private lazy val actor = system.actorOf(ScoringActor.props(overriddenConfig, new MockedScoringService, factory))
        private val message = getOutcomeForScoring(1)

        actor ! message
        childProbe.expectMsg(SimulationScoringActor.PerformScoring)

        await(actor ? ScoringActor.GetRunningCount) should be (1)
        await(actor ? ScoringActor.GetQueuedCount) should be (0)

        startedScoring should have size 1
        startedScoring should be (Seq(message))

        actor ! ScoringActor.CleanCalculations

        await(actor ? ScoringActor.GetRunningCount) should be (0)
        await(actor ? ScoringActor.GetQueuedCount) should be (0)

        actor ! PoisonPill
      }

      "kill actor if calculation exceeded limit and start new calculation from queue" in new WithSpecActor {
        private val overriddenConfig = config
          .withValue("io.towerstreet.attacksimulator.scoring.calculationTimeout", ConfigValueFactory.fromAnyRef(time.Duration.ofHours(-1)))
          .withValue("io.towerstreet.attacksimulator.scoring.maxConcurrentScoring", ConfigValueFactory.fromAnyRef(1))

        private lazy val actor = system.actorOf(ScoringActor.props(overriddenConfig, new MockedScoringService, factory))
        private val message = getOutcomeForScoring(1)
        private val message2 = getOutcomeForScoring(2)

        actor ! message
        childProbe.expectMsg(SimulationScoringActor.PerformScoring)

        actor ! message2

        await(actor ? ScoringActor.GetRunningCount) should be (1)
        await(actor ? ScoringActor.GetQueuedCount) should be (1)

        startedScoring should have size 1
        startedScoring should be (Seq(message))

        actor ! ScoringActor.CleanCalculations

        await(actor ? ScoringActor.GetRunningCount) should be (1)
        await(actor ? ScoringActor.GetQueuedCount) should be (0)

        startedScoring should have size 2
        startedScoring should be (Seq(message, message2))

        actor ! PoisonPill
      }

      "do nothing no calculation doesn't exceeded limit" in new WithSpecActor {
        private lazy val actor = system.actorOf(ScoringActor.props(config, new MockedScoringService, factory))
        private val message = getOutcomeForScoring(1)

        actor ! message
        childProbe.expectMsg(SimulationScoringActor.PerformScoring)

        await(actor ? ScoringActor.GetRunningCount) should be (1)
        await(actor ? ScoringActor.GetQueuedCount) should be (0)

        startedScoring should have size 1
        startedScoring should be (Seq(message))

        actor ! ScoringActor.CleanCalculations

        await(actor ? ScoringActor.GetRunningCount) should be (1)
        await(actor ? ScoringActor.GetQueuedCount) should be (0)

        actor ! PoisonPill
      }

      "do nothing if no calculation is running" in new WithSpecActor {
        private lazy val actor = system.actorOf(ScoringActor.props(config, new MockedScoringService, factory))

        actor ! ScoringActor.CleanCalculations

        await(actor ? ScoringActor.GetRunningCount) should be (0)
        await(actor ? ScoringActor.GetQueuedCount) should be (0)

        startedScoring should have size 0

        actor ! PoisonPill
      }
    }
  }
}
