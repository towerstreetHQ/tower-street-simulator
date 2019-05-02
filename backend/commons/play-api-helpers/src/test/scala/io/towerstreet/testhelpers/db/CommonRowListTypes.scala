package io.towerstreet.testhelpers.db

import java.time.LocalDateTime
import java.util.UUID

import acolyte.jdbc.RowLists
import acolyte.jdbc.RowLists._

/**
  * Mixin trait with basic row types to be shared across the project tests. Here should be contained mainly row types for
  * DB tables and views. Join or query specific row lists should be defined in project where they are used.
  */
trait CommonRowListTypes {
  lazy val IntKeyRowList = rowList1(classOf[Int]).withLabel(1, "id")

  lazy val UuidRowType = rowList1(classOf[UUID])


  // ////////////////////////////////
  // public schema

  lazy val CustomerRowType =
    rowList5(classOf[Int], classOf[String], classOf[String], classOf[Boolean], classOf[Boolean])

  lazy val CustomerAssessmentRowType = rowList5(
    classOf[Int], classOf[Int], classOf[Int], classOf[LocalDateTime], classOf[LocalDateTime]
  )

  lazy val ApiKeyRowType = rowList10(
    classOf[Int], classOf[Int], classOf[UUID], classOf[LocalDateTime], classOf[LocalDateTime],
    classOf[LocalDateTime], classOf[Integer], classOf[Integer], classOf[String], classOf[String]
  )

  lazy val UserAccountRowType = rowList7(classOf[Int], classOf[String], classOf[String],
    classOf[LocalDateTime], classOf[LocalDateTime], classOf[Integer], classOf[Boolean])

  lazy val CampaignVisitorRowType = rowList16(classOf[Int], classOf[Int], classOf[LocalDateTime], classOf[String],
    classOf[String], classOf[String], classOf[String], classOf[String], classOf[String], classOf[Integer], classOf[String],
    classOf[Boolean], classOf[LocalDateTime], classOf[LocalDateTime], classOf[Integer], classOf[Boolean])

  lazy val CampaignShareHistogramRowType = rowList3(classOf[Int], classOf[String], classOf[Int])

  // ////////////////////////////////
  // attacksimulator schema

  lazy val SimulationUserRowType =
    rowList4(classOf[Int], classOf[Int], classOf[String], classOf[Integer])

  lazy val SimulationRowType =
    rowList6(classOf[Int], classOf[Int], classOf[UUID], classOf[String], classOf[Int], classOf[Int])

  lazy val SimulationOutcomeRowType = rowList6(
    classOf[Int], classOf[Int], classOf[UUID], classOf[LocalDateTime], classOf[LocalDateTime], classOf[LocalDateTime]
  )

  lazy val RunnerRowType =
    rowList2(classOf[Int], classOf[String])

  lazy val TaskRowType =
    rowList4(classOf[Int], classOf[Int], classOf[String], classOf[String])

  lazy val UrlTestRowType = RowLists.rowList7(classOf[Int], classOf[Integer], classOf[Int],
    classOf[String], classOf[Long], classOf[java.time.Duration], classOf[LocalDateTime])

  lazy val TaskForSimulationRowType = rowList7(
    classOf[Int], classOf[Int], classOf[Int], classOf[String], classOf[String], classOf[String], classOf[String]
  )

  lazy val SimulationTemplateRowType = RowLists.rowList4(classOf[Int], classOf[LocalDateTime], classOf[Int],
    classOf[String])

  lazy val TestCaseRowType = rowList4(classOf[Int], classOf[String], classOf[String], classOf[String])

  lazy val CampaignVisitorSimulationRowType = rowList14(
    classOf[Int], classOf[Int], classOf[Int], classOf[UUID], classOf[Int], classOf[Int], classOf[LocalDateTime],
    classOf[LocalDateTime], classOf[LocalDateTime], classOf[Int], classOf[LocalDateTime], classOf[LocalDateTime],
    classOf[Boolean], classOf[Int]
  )

  // ////////////////////////////////
  // scoring schema

  lazy val ScoringDefinitionRowType = RowLists.rowList8(classOf[Int], classOf[String], classOf[String], classOf[String],
    classOf[String], classOf[String], classOf[Int], classOf[String])

  lazy val ScoringOutcomeRowType = RowLists.rowList6(classOf[Int], classOf[Int], classOf[Int],
    classOf[LocalDateTime], classOf[LocalDateTime], classOf[Integer])

}

object CommonRowListTypes extends CommonRowListTypes
