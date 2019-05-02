package io.towerstreet.attacksimulator.dao

import java.time.LocalDateTime

import io.towerstreet.dao.{AbstractDAO, DAOHelpers}
import io.towerstreet.slick.models.generated.attacksimulator.Model.{SimulationOutcomeId, SimulationTemplateId}
import io.towerstreet.slick.models.generated.scoring.Model._
import io.towerstreet.slick.models.generated.scoring.Tables
import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.ExecutionContext

class ScoringDAO @Inject()(val dbConfigProvider: DatabaseConfigProvider)
                          (implicit ec: ExecutionContext)
  extends AbstractDAO
    with DAOHelpers
{
  import api._
  import implicits._

  def getSimulationOutcomeForScoringQuery(startDelay: LocalDateTime,
                                          restartTimeout: LocalDateTime,
                                          outcomesLimit: Int,
                                          retriesLimit: Int
                                         ): DBIO[Seq[SimulationOutcomeForScoring]] = {
    Tables.SimulationOutcomeForScoringTable
      .filter(t =>
        // Only not completed scoring
        t.scoringFinishedAt.isEmpty &&
          // Only finished outcomes before delay
          t.outcomeFinishedAt.nonEmpty && t.outcomeFinishedAt < startDelay &&
          // Only scoring which haven't started or is after restart timeout
          (t.scoringQueuedAt.isEmpty || t.scoringQueuedAt < restartTimeout) &&
          // Only scoring which haven't exceeded number of retries
          (t.retries.isEmpty || t.retries < retriesLimit)
      ).take(outcomesLimit)
      .result
  }

  def getScoringConfigForTemplate(simulationTemplateId: SimulationTemplateId): DBIO[Seq[SimulationScoringConfig]] = {
    Tables.SimulationScoringConfigTable
      .filter(_.simulationTemplateId === simulationTemplateId)
      .result
  }

  def getScoringDefinitionsByIds(scoringDefinitionIds: Set[ScoringDefinitionId]): DBIO[Seq[ScoringDefinition]] = {
    Tables.ScoringDefinitionTable
      .filter(_.id.inSetBind(scoringDefinitionIds))
      .result
  }

  def getScoringOutcomeForSimulation(simulationOutcomeId: SimulationOutcomeId): DBIO[ScoringOutcome] = {
    Tables.ScoringOutcomeTable
      .filter(_.simulationOutcomeId === simulationOutcomeId)
      .take(1)
      .result
      .mapSingleResult()
  }

  def insertScoringOutcomes(scoringOutcome: Seq[ScoringOutcome]): DBIO[Seq[ScoringOutcome]] = {
    Tables.ScoringOutcomeTableInsert ++= scoringOutcome
  }

  def insertScoringOutcome(scoringOutcome: ScoringOutcome): DBIO[ScoringOutcome] = {
    Tables.ScoringOutcomeTableInsert += scoringOutcome
  }

  def insertScoringResults(scoringResults: Seq[ScoringResult]): DBIO[Seq[ScoringResult]] = {
    Tables.ScoringResultTableInsert ++= scoringResults
  }

  def updateQueuedAt(scoringOutcomeIds: Seq[ScoringOutcomeId], queuedAt: LocalDateTime): DBIO[Int] = {
    val ids: Seq[Int] = scoringOutcomeIds.map(_.intValue())

    sql"""UPDATE "scoring"."scoring_outcome"
          SET "queued_at" = $queuedAt, "retries" = coalesce("retries" + 1, 1)
          WHERE id = ANY($ids)
       """.asUpdate
  }

  def updateFinishedAt(scoringOutcomeId: ScoringOutcomeId, finishedAt: LocalDateTime): DBIO[Int] = {
    Tables.ScoringOutcomeTable
      .filter(_.id === scoringOutcomeId)
      .map(_.finishedAt)
      .update(Some(finishedAt))
  }
}
