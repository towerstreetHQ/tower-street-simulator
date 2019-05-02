package io.towerstreet.attacksimulator.testhelpers.db

import java.time.LocalDateTime
import java.util.UUID

import acolyte.jdbc.Implicits._
import io.towerstreet.testhelpers.db.CommonResults

object Results extends CommonResults {

  lazy val tasks =(RowListTypes.TaskRowType
    :+ (1, 1, "task-01", """{"f":"f"}""")
    :+ (2, 1, "task-02", null)
    :+ (3, 2, "task-03", """{"f":"f"}""")
    ).asResult

  lazy val singleTask =(RowListTypes.TaskRowType
    :+ (1, 1, "task-01", """{"f":"f"}""")
    ).asResult

  lazy val singleTaskWithRunner =(RowListTypes.TaskWithRunnerRowType
    :+ (1, 1, "task-01", """{"f":"f"}""", 1, "runner-01")
    ).asResult

  lazy val taskForSimulation = (RowListTypes.TaskForSimulationRowType
    :+ (1, 1, 1, "task-01", "runner-01", "test-case-01", """{"f":"f"}""")
    :+ (2, 1, 2, "task-02", "runner-01", null, null)
    :+ (3, 1, 3, "task-03", "runner-02", "test-case-02", """{"f":"f"}""")
    ).asResult

  def singleOutcomeWithSimulation(token: UUID, isFinished: Boolean = false) = {
    val now = LocalDateTime.now()
    (RowListTypes.OutcomeWithSimulationRowType
      :+ (1, 1, token, now, if (isFinished) now else null, now, 1, 1, token, "simulation-01", 1, 1)
      ).asResult
  }

  lazy val networkSegmentIp = (RowListTypes.NetworkSegmentIpType
    :+ ("10.0.11.0", 26)
    :+ ("10.0.12.0", 26)
    :+ ("10.0.13.0", 26)
  ).asResult
}
