package io.towerstreet.attacksimulator.models

import com.github.vitalsoftware.macros.{json, jsonDefaults}

object PortScan {
  @json
  case class PortScanResult(opened: Boolean, duration: Double)

  @jsonDefaults
  case class PortScanParameters(portScanTimeout: Int = 3000,
                                ports: Seq[Int] = Seq(80, 443, 445, 65534),
                                portScanDelay: Int = 40000
                               )

  @jsonDefaults
  case class PortScanScoringParameters(ports: Set[String] = Set(),
                                       requiredRuns: Option[Int] = None
                                      )

  // Port scan test result
  @json
  case class PortScanResultWitness(port: String, ips: Seq[String])
}
