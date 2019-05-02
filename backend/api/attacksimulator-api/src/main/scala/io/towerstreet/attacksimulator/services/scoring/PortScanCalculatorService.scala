package io.towerstreet.attacksimulator.services.scoring

import java.time.LocalDateTime

import io.towerstreet.attacksimulator.models.PortScan.{PortScanResult, PortScanResultWitness, PortScanScoringParameters}
import io.towerstreet.attacksimulator.models.Scoring.TaskWithResult
import io.towerstreet.logging.Logging
import io.towerstreet.slick.models.generated.attacksimulator.Model.TaskId
import io.towerstreet.slick.models.generated.scoring.Model.{ScoringDefinition, ScoringOutcomeId, ScoringResult, ScoringResultId}
import io.towerstreet.slick.models.scoring.enums.ScoringResultType
import play.api.libs.json.{JsObject, Json}
import shapeless.tag
import shapeless.tag.@@

import scala.concurrent.Future

object PortScanCalculatorService {

  sealed trait IpTag
  sealed trait PortTag

  type Ip = String @@ IpTag
  type Port = String @@ PortTag

  private[scoring] val TestPort = tag[PortTag]("65534")
  private[scoring] val OpenedLimitDuration = 10
  private[scoring] val RtsLimitDuration = 20

  case class PortResult(ip: Ip, port: Port, opened: Boolean, duration: Double)

  object PortResult {
    def apply(ip: String, port: String, value: PortScanResult): PortResult =
      new PortResult(tag[IpTag][String](ip), tag[PortTag][String](port), value.opened, value.duration)
  }
}

class PortScanCalculatorService
  extends Logging
    with ScoringCalculationHelpers
{

  import PortScanCalculatorService._

  /**
    * Determines whether there are opened ports in client network. Calculation uses multiple simulations to obtain that
    * information.
    *
    * Port is Marked as opened if is identified as opened for all simulations (this needs to remove false positives).
    * Following is tested for each simulation: 10ms < SCAN_DURATION < SCAN_TIMEOUT.
    *
    * Removes all results for IPs where port 65534 is found to be opened as they are false positives (like scanning ip
    * belonging to network like 192.168.0.0 ip address which is blocked almost immediately in chrome and results
    * in open port).
    */
  def calculateOpenedPortResults(scoringOutcomeId: ScoringOutcomeId,
                                 scoringDefinition: ScoringDefinition,
                                 taskOutcomes: Seq[(TaskId, Option[TaskWithResult])]
                                ): Future[ScoringResult] = {
    def isAlwaysOpened(ports: Seq[PortResult]) = ports.forall(isPortOpened)

    calculateResults(scoringOutcomeId, scoringDefinition, taskOutcomes, isAlwaysOpened)
  }

  /**
    * Determines whether there are ports which occasionally sends RTS packet. Calculation uses at least one task
    * simulation to obtain result.
    *
    * If during the 2 port scans once the port is marked open and on second scan it's marked as closed reaching
    * timeout then it's closed port responding with RST packet -> hence host firewall not present or not in drop mode.
    *
    * Port is marked as sending RTS if:
    *   1) For one run SCAN_DURATION > SCAN_TIMEOUT
    *   2) For another run SCAN_DURATION < 20ms
    *
    */
  def calculateMissingFirewallResults(scoringOutcomeId: ScoringOutcomeId,
                                      scoringDefinition: ScoringDefinition,
                                      taskOutcomes: Seq[(TaskId, Option[TaskWithResult])]
                                     ): Future[ScoringResult] = {

    def isMissingFirewall(ports: Seq[PortResult]) = {
      val hasTimeout = ports.exists(! _.opened)
      val hasRtsPacket = ports.exists(_.duration <= RtsLimitDuration)

      hasTimeout && hasRtsPacket
    }

    calculateResults(scoringOutcomeId, scoringDefinition, taskOutcomes, isMissingFirewall)
  }

  /**
    * Main method to calculate combined result of port scan. Method will gather raw result parse them and aggregate
    * test results for same port and IP. Given function is then used to evaluate overall result for single port and IP.
    *
    * Method can produce unknown result when task outcomes are empty or all ports or ips were filtered out.
    *
    * Scoring definition can specify list of ports to be tested or number of task runs to be required for accept
    * testing.
    */
  private[scoring] def calculateResults(scoringOutcomeId: ScoringOutcomeId,
                                        scoringDefinition: ScoringDefinition,
                                        taskOutcomes: Seq[(TaskId, Option[TaskWithResult])],
                                        isPortInvalid: Seq[PortResult] => Boolean
                                       ) = {

    val parameters = parseScoringParameters(scoringDefinition, PortScanScoringParameters())
    val rawResults = getRawResults(taskOutcomes)

    // We can continue only if we have enough data to process
    // If calculation requires multiple results then require it, or we need just some results
    val hasEnoughData = parameters.requiredRuns.fold(rawResults.nonEmpty)(_ <= rawResults.size)

    val witnessesOpt =
      if (hasEnoughData) {
        // If we have data then parse them and continue only if there is anything to calculate
        val results = parseResults(rawResults, parameters.ports)
        val aggregated = aggregateResults(results)

        if (aggregated.nonEmpty) {
          // Provide calculation and return witnesses of invalid ports
          val w = findInvalidPorts(aggregated, isPortInvalid)

          Some(w)
        } else None
      } else None

    // Unknown if not enough data otherwise true if no invalid port
    val result = witnessesOpt.fold[ScoringResultType](ScoringResultType.Unknown)(
      w => ScoringResultType(w.isEmpty)
    )

    Future.successful(ScoringResult(ScoringResultId(0), scoringDefinition.id, scoringOutcomeId, LocalDateTime.now(),
      result, witnessesOpt.map(w => Json.toJson(w))))
  }

  /**
    * Ensures json objects with raw results for specified tasks. Excluded tasks with invalid results which would
    * cause inconclusive tests.
    *
    * Task result should be two-level JSON object. Root fields represent IP addresses, next level field
    * represents ports and last level represents scanning result [[PortScanResult]].
    *
    * {
    *   "10.0.11.0": {
    *     "80": {"opened": true, "duration": 23},
    *     "443": {"opened": false, "duration": 3003},
    *   },
    *   "10.0.11.20": { ... },
    *   ...
    * }
    */
  private[scoring] def getRawResults(taskOutcomes: Seq[(TaskId, Option[TaskWithResult])]) = {
    taskOutcomes.flatMap { to =>
      for {
        taskWithResults <- to._2
        taskResult <- taskWithResults.outcome.taskResult
        resultObj <- taskResult.validate[JsObject].asOpt
      } yield resultObj
    }
  }

  /**
    * Parses raw tasks result to sequence of [[PortScanResult]] which is needed for processing. Method ill include ports
    * specified in PortsToTest parameter. If parameter is empty then all ports are included.
    */
  private[scoring] def parseResults(taskResults: Seq[JsObject],
                                    portsToTest: Set[String]
                                   ) = {

    // Port is kept for processing if it is special test port or if it is included in parameters (empty parameters
    // means that all ports are used)
    def includePort(port: String) = portsToTest.isEmpty || portsToTest.contains(port) || port == TestPort

    taskResults.flatMap {
      // Iterate over ip addresses
      _.fields.flatMap {
        case (ip, ports: JsObject) =>

          // Iterate over port results for IP address
          ports.fields.flatMap {
            case (port, portResultJsObject) if includePort(port) =>
              // Parse port and wrap it to result obj
              portResultJsObject.validate[PortScanResult]
                .asOpt.map(PortResult.apply(ip, port, _))

            // Port should be excluded
            case _ => None
          }

        // Ip address result is not JS object
        case _ => None
      }
    }
  }

  /**
    * Aggregates port results by Port and Ip address + removes all results for broken IP addresses. Broken IP means
    * that it successfully connected to always invalid port so testing is inconclusive.
    */
  private[scoring] def aggregateResults(portResults: Seq[PortResult]
                                       ): Map[Port, Map[Ip, Seq[PortResult]]] = {
    // Identify IPs which should be excluded from calculation
    val excludedIps = portResults
      .flatMap { r =>
        if (r.port == TestPort && isPortOpened(r)) Some(r.ip)
        else None
      }.toSet

    portResults
      .filterNot(r => excludedIps.contains(r.ip) || r.port == TestPort)
      .groupBy(_.port).flatMap {
        case (port, ips) if ips.nonEmpty =>
          Some(port -> ips.groupBy(_.ip))

        case _ => None
      }
  }

  /**
    * For each port and IP address test validity (if scoring passes or fails). Ports with at least one invalid IP are
    * returned together with all invalid occurrences.
    */
  private[scoring] def findInvalidPorts(aggregated: Map[Port, Map[Ip, Seq[PortResult]]],
                                        isInvalid: Seq[PortResult] => Boolean
                                       ) = {
    aggregated.flatMap {
      case (port, ips) =>
        val openedIps = ips.toSeq.flatMap {
          case (ip, portResults) if isInvalid(portResults) =>
            Some(ip)

          case _ => None
        }

        if (openedIps.nonEmpty) {
          Some(PortScanResultWitness(port, openedIps))
        } else {
          None
        }
    }
  }

  private def isPortOpened(r: PortResult) = {
    r.opened && r.duration >= OpenedLimitDuration
  }
}
