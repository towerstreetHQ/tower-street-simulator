package io.towerstreet.attacksimulator.services.scoring

import io.towerstreet.attacksimulator.models.PortScan.{PortScanResult, PortScanResultWitness, PortScanScoringParameters}
import io.towerstreet.attacksimulator.models.Scoring.TaskWithResult
import io.towerstreet.attacksimulator.services.scoring.PortScanCalculatorService.{IpTag, PortResult, PortTag}
import io.towerstreet.slick.models.generated.attacksimulator.Model._
import io.towerstreet.slick.models.generated.scoring.Model.{ScoringDefinition, ScoringDefinitionId, ScoringOutcomeId}
import io.towerstreet.slick.models.scoring.enums.{ScoringCategory, ScoringResultType, ScoringType}
import io.towerstreet.testhelpers.WithApplicationSpec
import io.towerstreet.testhelpers.WithApplicationSpec.WithService
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import shapeless.tag

class PortScanCalculatorServiceTest
  extends PlaySpec
    with FutureAwaits
    with DefaultAwaitTimeout
{
  class WithSpecApplication()
    extends WithApplicationSpec
      with WithService[PortScanCalculatorService]

  private val resultConnected = PortScanResult(opened = true, 100)
  private val resultTimeout = PortScanResult(opened = false, 3000)
  private val resultRst = PortScanResult(opened = true, 15)
  private val resultReset = PortScanResult(opened = true, 5)

  private val p1_1_80 = PortResult("127.0.0.1", "80", resultConnected)
  private val p1_2_80 = PortResult("127.0.0.1", "80", resultTimeout)
  private val p1_1_443 = PortResult("127.0.0.1", "443", resultConnected)
  private val p1_1_65534 = PortResult("127.0.0.1", "65534", resultConnected)
  private val p2_1_80 = PortResult("127.0.0.2", "80", resultConnected)
  private val p3_1_80 = PortResult("127.0.0.3", "80", resultConnected)
  private val p3_2_80 = PortResult("127.0.0.3", "80", resultReset)
  private val p3_1_65534 = PortResult("127.0.0.3", "65534", resultTimeout)
  private val p4_1_80 = PortResult("127.0.0.4", "80", resultConnected)
  private val p4_1_65534 = PortResult("127.0.0.4", "65534", resultReset)
  
  private val port80 = tag[PortTag][String]("80")
  private val port443 = tag[PortTag][String]("443")

  private val ip1 = tag[IpTag][String]("127.0.0.1")
  private val ip2 = tag[IpTag][String]("127.0.0.2")
  private val ip3 = tag[IpTag][String]("127.0.0.3")
  private val ip4 = tag[IpTag][String]("127.0.0.4")

  def getScoringDefinition(parameters: Option[PortScanScoringParameters] = None) = {
    ScoringDefinition(ScoringDefinitionId(1), "", ScoringType.OpenPortScanResult, "", parameters.map(Json.toJson(_)), "", 1, ScoringCategory.Cis)
  }

  def existNotOpened(ports: Seq[PortResult]) = ports.exists(!_.opened)

  private def getTestResult(id: Int,
                            taskKey: Option[String] = None,
                            isSuccess: Boolean = true,
                            hasResult: Boolean = true,
                            parameters: Option[PortScanScoringParameters] = None,
                            taskResult: Option[JsValue] = None
                           ) = {
    if (hasResult) {


      TaskId(id) -> Some(TaskWithResult(
        Task(TaskId(id), RunnerId(0), taskKey.getOrElse(s"task-$id"), parameters.map(p => Json.toJson(p))),
        OutcomeTaskResult(OutcomeTaskResultId(id), SimulationOutcomeId(0), TaskId(id), isSuccess, None, taskResult, None),
          None)
      )
    } else {
      TaskId(id) -> None
    }
  }

  private def getRawResults(results: Map[String, Map[String, PortScanResult]]) = {
    JsObject(
      results.map {
        case (ip, ports) =>
          ip -> JsObject(ports.map {
            case (port, portScanResult) =>
              port -> Json.toJson(portScanResult)
          })
      }
    )
  }

  "PortScanCalculatorService" should {

    "getRawResults" should {
      "obtain valid result and filter out invalid" in new WithSpecApplication {
        val r = service.getRawResults(Seq(
          getTestResult(1, taskResult = Some(Json.obj("f" -> 1))),
          getTestResult(2, hasResult = false),
          getTestResult(3),
          getTestResult(4, taskResult = Some(JsNumber(4))),
        ))

        r mustBe Seq(Json.obj("f" -> 1))
      }
    }

    "parseResults" should {
      "obtain all port results of raw json data" in new WithSpecApplication {
        val rawResults = Seq(
          getRawResults(Map(
            "127.0.0.1" -> Map("80" -> resultConnected, "443" -> resultConnected, "65534" -> resultTimeout),
            "127.0.0.2" -> Map("80" -> resultConnected),
            "127.0.0.3" -> Map()
          )),
          getRawResults(Map(
            "127.0.0.1" -> Map("80" -> resultTimeout),
            "127.0.0.4" -> Map()
          )),
          Json.obj(
            "127.0.0.10" -> JsNumber(1),
            "127.0.0.11" -> JsObject(Seq()),
            "127.0.0.12" -> Json.arr("80", "443"),
            "127.0.0.13" -> Json.obj("80" -> JsNumber(2))
          ),
          JsObject(Seq())
        )

        val r = service.parseResults(rawResults, Set.empty)

        r mustBe Seq(
          PortResult("127.0.0.1", "80", resultConnected),
          PortResult("127.0.0.1", "443", resultConnected),
          PortResult("127.0.0.1", "65534", resultTimeout),
          PortResult("127.0.0.2", "80", resultConnected),
          PortResult("127.0.0.1", "80", resultTimeout),
        )
      }

      "obtain only included ports port results of raw json data" in new WithSpecApplication {
        val rawResults = Seq(
          getRawResults(Map(
            "127.0.0.1" -> Map("80" -> resultConnected, "443" -> resultConnected, "65534" -> resultTimeout),
            "127.0.0.2" -> Map("443" -> resultConnected),
            "127.0.0.3" -> Map("90" -> resultConnected),
            "127.0.0.4" -> Map()
          )),
          getRawResults(Map(
            "127.0.0.1" -> Map("80" -> resultTimeout),
            "127.0.0.5" -> Map("90" -> resultConnected),
            "127.0.0.6" -> Map()
          ))
        )

        val r = service.parseResults(rawResults, Set("80", "90"))

        r mustBe Seq(
          PortResult("127.0.0.1", "80", resultConnected),
          PortResult("127.0.0.1", "65534", resultTimeout),
          PortResult("127.0.0.3", "90", resultConnected),
          PortResult("127.0.0.1", "80", resultTimeout),
          PortResult("127.0.0.5", "90", resultConnected)
        )
      }
    }

    "aggregateResults" should {
      "aggregate results by port and IP address" in new WithSpecApplication {
        val r = service.aggregateResults(Seq(p1_1_80, p1_2_80, p1_1_443, p2_1_80))

        r mustBe Map(
          port80 -> Map(ip1 -> Seq(p1_1_80, p1_2_80), ip2 -> Seq(p2_1_80)),
          port443 -> Map(ip1 -> Seq(p1_1_443))
        )
      }

      "exclude IP addresses with port 65534 connected" in new WithSpecApplication {
        val r = service.aggregateResults(Seq(p1_1_80, p1_2_80, p1_1_443, p1_1_65534, p2_1_80, p3_1_80,
          p3_1_65534, p4_1_80, p4_1_65534))

        r mustBe Map(
          port80 -> Map(ip2 -> Seq(p2_1_80), ip3 -> Seq(p3_1_80), ip4 -> Seq(p4_1_80))
        )
      }
    }

    "findInvalidPorts" should {
      "identify opened ports" in new WithSpecApplication {
        val r = service.findInvalidPorts(
          Map(
            port80 -> Map(ip1 -> Seq(p1_1_80, p1_2_80), ip2 -> Seq(p2_1_80), ip3 -> Seq(p3_1_80, p3_2_80)),
            port443 -> Map(ip1 -> Seq(p1_1_443))
          ),
          _.size >= 2
        )

        r mustBe Seq(
          PortScanResultWitness(port80, Seq(ip1, ip3))
        )
      }
    }

    "calculateResults" should {
      "calculate true result if all ports connected" in new WithSpecApplication {
        val rawResults = Seq(
          getTestResult(1, taskResult = Some(
            getRawResults(Map(
              "127.0.0.1" -> Map("80" -> resultConnected, "443" -> resultConnected),
              "127.0.0.2" -> Map("80" -> resultConnected)
            ))
          ))
        )
        val r = await(service.calculateResults(ScoringOutcomeId(1), getScoringDefinition(), rawResults, existNotOpened))

        r.result mustBe ScoringResultType.True
        r.resultParameters mustBe Some(JsArray())
      }
      "calculate false result if one port is not connected" in new WithSpecApplication {
        val rawResults = Seq(
          getTestResult(1, taskResult = Some(
            getRawResults(Map(
              "127.0.0.1" -> Map("80" -> resultTimeout, "443" -> resultConnected),
              "127.0.0.2" -> Map("80" -> resultConnected)
            ))
          ))
        )
        val r = await(service.calculateResults(ScoringOutcomeId(1), getScoringDefinition(), rawResults, existNotOpened))

        r.result mustBe ScoringResultType.False
        r.resultParameters mustBe Some(JsArray(Seq(
          Json.toJson(PortScanResultWitness(port80, Seq(ip1)))
        )))
      }
      "return unknown result if there are no data for processing" in new WithSpecApplication {
        val rawResults = Seq()
        val r = await(service.calculateResults(ScoringOutcomeId(1), getScoringDefinition(), rawResults, existNotOpened))

        r.result mustBe ScoringResultType.Unknown
        r.resultParameters mustBe None
      }
      "return unknown result if number of results is bellow required threshold" in new WithSpecApplication {
        val rawResults = Seq(
          getTestResult(1, taskResult = Some(
            getRawResults(Map(
              "127.0.0.1" -> Map("80" -> resultTimeout, "443" -> resultConnected),
              "127.0.0.2" -> Map("80" -> resultConnected)
            ))
          ))
        )
        val r = await(service.calculateResults(ScoringOutcomeId(1), getScoringDefinition(
          Some(PortScanScoringParameters(requiredRuns = Some(2)))), rawResults, existNotOpened))

        r.result mustBe ScoringResultType.Unknown
        r.resultParameters mustBe None
      }
      "return unknown result if all ports has been filtered out" in new WithSpecApplication {
        val rawResults = Seq(
          getTestResult(1, taskResult = Some(
            getRawResults(Map(
              "127.0.0.1" -> Map("80" -> resultTimeout, "443" -> resultConnected),
              "127.0.0.2" -> Map("80" -> resultConnected)
            ))
          ))
        )
        val r = await(service.calculateResults(ScoringOutcomeId(1), getScoringDefinition(
          Some(PortScanScoringParameters(ports = Set("1000")))), rawResults, existNotOpened))

        r.result mustBe ScoringResultType.Unknown
        r.resultParameters mustBe None
      }
      "return unknown result if all IPs has been filtered out" in new WithSpecApplication {
        val rawResults = Seq(
          getTestResult(1, taskResult = Some(
            getRawResults(Map(
              "127.0.0.1" -> Map("80" -> resultTimeout, "443" -> resultConnected, "65534" -> resultConnected),
              "127.0.0.2" -> Map("80" -> resultConnected, "65534" -> resultConnected)
            ))
          ))
        )
        val r = await(service.calculateResults(ScoringOutcomeId(1), getScoringDefinition(), rawResults, existNotOpened))

        r.result mustBe ScoringResultType.Unknown
        r.resultParameters mustBe None
      }
      "return unknown result for old scan results" in new WithSpecApplication {
        val rawResults = Seq(
          getTestResult(1, taskResult = Some(
            Json.obj(
              "127.0.0.1" -> Json.arr("80", "443"),
              "127.0.0.2" -> Json.arr("80")
            )
          ))
        )
        val r = await(service.calculateResults(ScoringOutcomeId(1), getScoringDefinition(), rawResults, existNotOpened))

        r.result mustBe ScoringResultType.Unknown
        r.resultParameters mustBe None
      }
    }

    "calculateOpenedPortResults" should {
      "return true result if no port is opened" in new WithSpecApplication {
        val rawResults = Seq(
          getTestResult(1, taskResult = Some(
            getRawResults(Map(
              "127.0.0.1" -> Map("80" -> resultTimeout, "443" -> resultReset),
              "127.0.0.2" -> Map("80" -> resultTimeout)
            )),
          )),
          getTestResult(1, taskResult = Some(
            getRawResults(Map(
              "127.0.0.1" -> Map("80" -> resultTimeout, "443" -> resultReset),
              "127.0.0.2" -> Map("80" -> resultTimeout)
            )),
          ))
        )

        val r = await(service.calculateOpenedPortResults(ScoringOutcomeId(1), getScoringDefinition(), rawResults))

        r.result mustBe ScoringResultType.True
        r.resultParameters mustBe Some(JsArray())
      }
      "return false result if if there is opened port" in new WithSpecApplication {
        val rawResults = Seq(
          getTestResult(1, taskResult = Some(
            getRawResults(Map(
              "127.0.0.1" -> Map("80" -> resultConnected, "443" -> resultReset),
              "127.0.0.2" -> Map("80" -> resultConnected),
              "127.0.0.3" -> Map("80" -> resultConnected)
            )),
          )),
          getTestResult(1, taskResult = Some(
            getRawResults(Map(
              "127.0.0.1" -> Map("80" -> resultConnected, "443" -> resultReset),
              "127.0.0.2" -> Map("80" -> resultTimeout)
            )),
          ))
        )

        val r = await(service.calculateOpenedPortResults(ScoringOutcomeId(1), getScoringDefinition(), rawResults))

        r.result mustBe ScoringResultType.False
        r.resultParameters mustBe Some(JsArray(Seq(
          Json.toJson(PortScanResultWitness(port80, Seq(ip1, ip3)))
        )))
      }
    }

    "calculateMissingFirewallResults" should {
      "return true if ports not timeout and send RTS ot the same time" in new WithSpecApplication {
        val rawResults = Seq(
          getTestResult(1, taskResult = Some(
            getRawResults(Map(
              "127.0.0.1" -> Map("80" -> resultConnected, "443" -> resultReset),
              "127.0.0.2" -> Map("80" -> resultTimeout),
              "127.0.0.3" -> Map("80" -> resultRst),
              "127.0.0.4" -> Map("80" -> resultTimeout),
              "127.0.0.5" -> Map("80" -> resultRst)
            )),
          )),
          getTestResult(1, taskResult = Some(
            getRawResults(Map(
              "127.0.0.1" -> Map("80" -> resultConnected, "443" -> resultReset),
              "127.0.0.2" -> Map("80" -> resultConnected),
              "127.0.0.3" -> Map("80" -> resultRst)
            )),
          ))
        )

        val r = await(service.calculateMissingFirewallResults(ScoringOutcomeId(1), getScoringDefinition(), rawResults))

        r.result mustBe ScoringResultType.True
        r.resultParameters mustBe Some(JsArray())
      }

      "return true result if there is port with timeouts and sometimes sends RTS" in new WithSpecApplication {
        val rawResults = Seq(
          getTestResult(1, taskResult = Some(
            getRawResults(Map(
              "127.0.0.1" -> Map("80" -> resultTimeout, "443" -> resultConnected),
              "127.0.0.2" -> Map("80" -> resultRst),
              "127.0.0.3" -> Map("80" -> resultRst),
            )),
          )),
          getTestResult(1, taskResult = Some(
            getRawResults(Map(
              "127.0.0.1" -> Map("80" -> resultRst, "443" -> resultTimeout),
              "127.0.0.2" -> Map("80" -> resultTimeout)
            )),
          ))
        )

        val r = await(service.calculateMissingFirewallResults(ScoringOutcomeId(1), getScoringDefinition(), rawResults))

        r.result mustBe ScoringResultType.False
        r.resultParameters mustBe Some(JsArray(Seq(
          Json.toJson(PortScanResultWitness(port80, Seq(ip1, ip2)))
        )))
      }
    }

  }

}
