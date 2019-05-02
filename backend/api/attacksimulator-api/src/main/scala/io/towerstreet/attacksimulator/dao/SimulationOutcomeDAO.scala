package io.towerstreet.attacksimulator.dao

import java.time.LocalDateTime
import java.util.UUID

import io.towerstreet.attacksimulator.models.Scoring.TaskWithResult
import io.towerstreet.dao.{AbstractDAO, DAOHelpers}
import io.towerstreet.slick.models.generated.attacksimulator.Model._
import io.towerstreet.slick.models.generated.attacksimulator.Tables
import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SimulationOutcomeDAO @Inject()(val dbConfigProvider: DatabaseConfigProvider)
                                    (implicit ec: ExecutionContext)
  extends AbstractDAO
    with DAOHelpers
{

  import api._
  import implicits._

  def getOutcomeWithSimulationByTokenQuery(token: UUID): DBIO[(SimulationOutcome, Simulation)] = {
    val q = for {
      o <- Tables.SimulationOutcomeTable if o.token === token
      s <- Tables.SimulationTable if s.id === o.simulationId
    } yield (o, s)

    q.result.mapSingleResult()
  }

  def getOutcomeByToken(token: UUID): Future[SimulationOutcome] = {
    val q =
      Tables.SimulationOutcomeTable.filter(_.token === token)
        .result
        .mapSingleResult()

    db.run(q)
  }

  def insertWithRequest(simulationOutcome: SimulationOutcome, request: ClientRequest): Future[SimulationOutcome] = {
    val q = for {
      inserted <- Tables.SimulationOutcomeTableInsert += simulationOutcome
      _ <- Tables.ClientRequestTableInsert += request.copy(simulationOutcomeId = Some(inserted.id))
    } yield inserted
    db.run(q)
  }

  def updateLastPingAt(token: UUID, newLastPingAt: LocalDateTime): Future[Int] = {
    val q = Tables.SimulationOutcomeTable
      .filter(so => so.token === token && so.finishedAt.isEmpty)
      .map(_.lastPingAt)
      .update(newLastPingAt)

    db.run(q)
  }

  def finishRunningSimulationQuery(outcomeId: SimulationOutcomeId,
                                   finishedAt: LocalDateTime
                                  ): DBIO[Int] = {
    Tables.SimulationOutcomeTable
      .filter(_.id === outcomeId)
      .map(_.finishedAt)
      .update(Some(finishedAt))
  }

  def storeTestResult(testResult: OutcomeTaskResult, clientRequest: ClientRequest): Future[OutcomeTaskResult] = {
    val q = for {
      result <- Tables.OutcomeTaskResultTableInsert += testResult
      _ <- Tables.ClientRequestTableInsert += clientRequest
    } yield result

    db.run(q.transactionally)
  }

  def getTasksWithResults(outcomeId: SimulationOutcomeId): DBIO[Seq[TaskWithResult]] = {
    val q = Tables.OutcomeTaskResultTable
      .join(Tables.TaskTable).on(_.taskId === _.id)
      .joinLeft(Tables.UrlTestTable).on(_._1.urlTestId === _.id)
      .filter(_._1._1.simulationOutcomeId === outcomeId)

    q.result.map(_.map {
      case ((tr, t), u) => TaskWithResult(t, tr, u)
    })
  }
}