package io.towerstreet.testhelpers.db

import java.time.LocalDateTime
import java.util.UUID

import acolyte.jdbc.Implicits._

/**
  * Mixin trait with basic results of row types to be shared across the tests. Here should be contained mainly results for
  * DB tables and views.
  *
  * Join, query specific or special cases should be defined in project or test where they are used.
  */
trait CommonResults {
  def uuidResult(uuid: UUID) =
    (CommonRowListTypes.UuidRowType :+ uuid).asResult

  // ////////////////////////////////
  // public schema

  lazy val singleCustomer = (CommonRowListTypes.CustomerRowType
    :+ (1, "client-01", "client-01", false, true)
    ).asResult

  def getCustomerAssessment(companyId: Int = 1,
                            closedAt: Option[LocalDateTime] = None
                           ) = (CommonRowListTypes.CustomerAssessmentRowType
    :+ (companyId, 1, companyId, LocalDateTime.now, closedAt.orNull)
    ).asResult

  lazy val CustomerAssessment = getCustomerAssessment()

  def singleUserAccount(username: String,
                        passwordHash: String,
                        userId: Int = 1,
                        hasLastCustomer: Boolean = true,
                        deleted: Boolean = false
                       ) = {
    (CommonRowListTypes.UserAccountRowType
      :+ (userId, username, passwordHash, LocalDateTime.now(),
      if (deleted) LocalDateTime.now() else null,
      if (hasLastCustomer) 1 else null,
      true
    )
      ).asResult
  }

  def singleApiKey(apiKey: UUID) = {
    (CommonRowListTypes.ApiKeyRowType
      :+ (1, 1, apiKey, LocalDateTime.now(), null, null, null, null, null, null)
      ).asResult
  }


  // ////////////////////////////////
  // attacksimulator schema

  def singleSimulation(token: UUID, id: Int = 1, customer: Int = 1) = {
    (CommonRowListTypes.SimulationRowType
      :+ (id, customer, token, "simulation-01", 1, 1)
      ).asResult
  }

  def singleSimulationOutcome(token: UUID, isFinished: Boolean = false) = {
    val now = LocalDateTime.now()
    (CommonRowListTypes.SimulationOutcomeRowType
      :+ (1, 1, token, now, if (isFinished) now else null, now)
      ).asResult
  }

  lazy val SimulationTemplate =
    (CommonRowListTypes.SimulationTemplateRowType :+ (1, LocalDateTime.now(), 1, null)).asResult()
}
