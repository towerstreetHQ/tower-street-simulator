package io.towerstreet.attacksimulator.services.scoring

import io.towerstreet.attacksimulator.models.Scoring.{TaskWithResult, UrlTestResultWitness}
import io.towerstreet.attacksimulator.models.UrlTest.UrlTaskParameters
import io.towerstreet.logging.Logging
import io.towerstreet.slick.models.generated.attacksimulator.Model.{OutcomeTaskResult, TaskId, UrlTest}
import io.towerstreet.slick.models.generated.scoring.Model.{ScoringDefinition, ScoringOutcomeId, ScoringResult}
import javax.inject.Inject

import scala.concurrent.{ExecutionContext, Future}

object UrlTestCalculatorService {
  private[scoring] val MaxTimeoutKey = "cis-7.4-maxtimeout"
  private[scoring] val ResetTimeoutKey = "cis-7.4-resettimeout"
  private[scoring] val BaselineKey = "cis-7.4-baseline"

  private[scoring] val TimeoutKeys = Set(MaxTimeoutKey, ResetTimeoutKey, BaselineKey)

  case class Timeouts(maxTimeout: Double,
                      resetTimeout: Double,
                      baseline: Double
                     )
  object Timeouts {
    def apply(timeoutTasks: Seq[(TaskId, Option[TaskWithResult])]): Option[Timeouts] = {
      val timeoutsMap = timeoutTasks.flatMap(_._2.map(t => t.task.taskKey -> t.outcome.duration)).toMap

      for {
        maxTimeout <- timeoutsMap.get(MaxTimeoutKey).flatten
        resetTimeout <- timeoutsMap.get(ResetTimeoutKey).flatten
        baseline <- timeoutsMap.get(BaselineKey).flatten
      } yield Timeouts(maxTimeout.toMillis, resetTimeout.toMillis, baseline.toMillis)
    }
  }

  case class ConnectionResult(connected: Boolean,
                              ignored: Boolean
                             )
}

/**
  * Calculation service to determine whether tested system can or can't connect certain web services. Calculation uses
  * test tasks which was also verified by UrlTest mechanism - web services which are alive (can be connected from
  * our system). Alternatively system can use baseline timeouts to calculate success / failure of test task.
  *
  * Calculation uses [[UrlTaskParameters]] to select correct approach:
  *   1) Webserver with image (just marked that can connect)
  *   2) Webserver without image (connection delay needs to fit formula based on reset, max and baseline timeouts)
  *   3) No webserver (connection delay needs to fit formula based on max timeout)
  *
  * Notes:
  *   * Approaches 2 and 3 are less accurate and should be used only if necessary.
  *   * Approaches 2 and 3 requires presence of all timeout tasks for calculation. If any of timeout is missing then
  *     test is ignored.
  *   * Timeout tasks are not part of scoring, they are only used to get calculation values.
  */
class UrlTestCalculatorService @Inject()()(implicit ec: ExecutionContext)
  extends Logging
    with ScoringCalculationHelpers
{
  import UrlTestCalculatorService._

  def calculateResult(scoringOutcomeId: ScoringOutcomeId,
                      scoringDefinition: ScoringDefinition,
                      taskOutcomes: Seq[(TaskId, Option[TaskWithResult])]
                     ): Future[ScoringResult] = {

    // Filter out baseline tasks for measuring timeouts
    val (timeoutTasks, tasks) = taskOutcomes.partition(_._2.exists(t => TimeoutKeys.contains(t.task.taskKey)))
    val timeouts = Timeouts(timeoutTasks)

    logger.debug(s"Calculating URL test scoring, scoringOutcomeId: $scoringOutcomeId, scoringDefinition: ${scoringDefinition.scoringKey}, timeouts: $timeouts")

    // Contains overall results of all tasks used to get this scoring
    val witnesses = tasks.map {
      // Filter out only performed tasks
      case (id, Some(result@TaskWithResult(_, outcome, urlTest))) =>
        val connected = getConnected(result, timeouts)
        val canBackendConnect = urlTest.exists(canConnect)
        UrlTestResultWitness(id, connected.connected, canBackendConnect, connected.ignored, Some(outcome.id), urlTest.map(_.id))

      // Not mapped tasks are ignored
      case (id, None) =>
        UrlTestResultWitness(id, connected = false, backendConnected = false, ignored = true, None, None)
    }

    // Get results which are accessible from backend and fit definition criteria
    evaluateCombinedResults(scoringOutcomeId, scoringDefinition, witnesses)(! _.ignored)(_.connected)
  }

  private[scoring] def getConnected(result: TaskWithResult, timeouts: Option[Timeouts]) = {
    // If no parameter is specified then we just use defaults
    val parameters = parseWithDefault(result.task.parameters, UrlTaskParameters(""))

    logger.debug(s"getConnected, taskKey: ${result.task}, parameters: ${result.task.parameters}")

    if (parameters.hasWebServer && parameters.hasImage) {
      isConnectedWebserverImage(result.outcome, result.urlTest)
    } else if (parameters.hasWebServer) {
      isConnectedWebserverNoImage(result.outcome, timeouts, result.urlTest)
    } else {
      isConnectedNonWebserver(result.outcome, timeouts)
    }
  }

  /**
    * Calculates connection for host which has image on their web server. Just get information if image can be
    * downloaded from task result isSuccess. Task is marked as ignored if backend URL test wasn't successful.
    */
  private[scoring] def isConnectedWebserverImage(outcome: OutcomeTaskResult,
                                                 urlTest: Option[UrlTest]
                                                ) = {
    logger.trace(s"isConnectedWebserverImage, taskId: ${outcome.taskId}")

    val canBackendConnect = urlTest.exists(canConnect)
    ConnectionResult(
      connected = outcome.isSuccess,
      ignored = !canBackendConnect
    )
  }

  /**
    * Calculates connection for host which doesn't have image on its web server. Request duration needs to be less than
    * max timeout, higher than reset and not significantly slower than backend connection. This proves that request
    * actually got to server which started to serve it but failed to locate requested resource. Task is marked as
    * ignored if timeouts are missing or backend URL test wasn't successful.
    */
  private[scoring] def isConnectedWebserverNoImage(outcome: OutcomeTaskResult,
                                                   timeouts: Option[Timeouts],
                                                   urlTest: Option[UrlTest]
                                                  ) = {
    logger.trace(s"isConnectedWebserverImage, taskId: ${outcome.taskId}, duration: ${outcome.duration}, urlTest: ${urlTest.flatMap(_.duration)}")

    val connectedOpt = for {
      duration <- outcome.duration
      backendDuration <- urlTest.flatMap(_.duration)
      t <- timeouts
    } yield {
      val R = duration.toMillis
      val BT = backendDuration.toMillis
      val BST = t.baseline
      val RST = t.resetTimeout
      val MXT = t.maxTimeout

      notReset(R, RST) && notTimeout(R, MXT) && notBackendFaster(R, BT, BST)
    }

    ConnectionResult(
      connected = connectedOpt.getOrElse(false),
      ignored = connectedOpt.isEmpty
    )
  }

  /**
    * Calculates connection for hosts which don't have web server. Request duration needs to be less than max timeout.
    * This proves that request actually got to server which refused it immediately without starting to serve it. Task
    * is marked as ignored if timeouts are missing.
    */
  private[scoring] def isConnectedNonWebserver(outcome: OutcomeTaskResult,
                                               timeouts: Option[Timeouts]
                                              ) = {
    logger.trace(s"isConnectedNonWebserver, taskId: ${outcome.taskId}, duration: ${outcome.duration}")

    val connectedOpt = for {
      duration <- outcome.duration
      t <- timeouts
    } yield {
      val R = duration.toMillis
      val MXT = t.maxTimeout

      notTimeout(R, MXT)
    }

    ConnectionResult(
      connected = connectedOpt.getOrElse(false),
      ignored = connectedOpt.isEmpty
    )
  }

  /**
    * Determines whether request duration is higher than reset timeout (so connection wasn't killed on road).
    */
  private def notReset(R: Double, RST: Double) = R > 1.5 * RST

  /**
    * Determines whether request duration is below maximum timeout (so connection wasn't got stuck on road).
    */
  private def notTimeout(R: Double, MXT: Double) = R < MXT / 2

  /**
    * Determines whether request duration is smaller than connection from backend.
    */
  private def notBackendFaster(R: Double, BT: Double, BST: Double) = R < (BT + BST) * 1.4

  /**
    * Determines whether this URL can be connected from backend.
    */
  private def canConnect(urlTest: UrlTest) = urlTest.duration.isDefined
}
