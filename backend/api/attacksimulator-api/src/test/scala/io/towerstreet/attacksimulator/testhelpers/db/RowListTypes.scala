package io.towerstreet.attacksimulator.testhelpers.db

import java.time.LocalDateTime
import java.util.UUID

import acolyte.jdbc.RowLists
import acolyte.jdbc.RowLists._
import io.towerstreet.testhelpers.db.CommonRowListTypes

object RowListTypes extends CommonRowListTypes {

  lazy val TaskWithRunnerRowType =
    rowList6(classOf[Int], classOf[Int], classOf[String], classOf[String], classOf[Int], classOf[String])

  lazy val OutcomeWithSimulationRowType = rowList12(
    // SimulationOutcome
    classOf[Int], classOf[Int], classOf[UUID], classOf[LocalDateTime], classOf[LocalDateTime], classOf[LocalDateTime],
    // Simulation
    classOf[Int], classOf[Int], classOf[UUID], classOf[String], classOf[Int], classOf[Int]
  )

  lazy val NetworkSegmentIpType = rowList2(
    classOf[String], classOf[Int]
  )

  lazy val SimulationOutcomeForScoringRowType = RowLists.rowList9(classOf[Int], classOf[LocalDateTime], classOf[LocalDateTime],
    classOf[Int], classOf[Integer], classOf[LocalDateTime], classOf[LocalDateTime], classOf[Int], classOf[Integer])

  lazy val TasksWithResultsRowType = RowLists.rowList20(
    // Task result
    classOf[Int], classOf[Int], classOf[Int], classOf[Boolean], classOf[String], classOf[String], classOf[java.time.Duration], classOf[Integer],
    // Task
    classOf[Int], classOf[Int], classOf[String], classOf[String],
    // Url Test
    classOf[Integer], classOf[Integer], classOf[Integer], classOf[Integer], classOf[String], classOf[java.lang.Long], classOf[java.time.Duration], classOf[LocalDateTime]
  )

  lazy val SimulationScoringConfigRowType = RowLists.rowList3(classOf[Int], classOf[Int], classOf[Int])

}
