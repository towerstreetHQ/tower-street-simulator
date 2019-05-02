package io.towerstreet.attacksimulator.dao

import com.github.tminglei.slickpg.InetString
import io.towerstreet.dao.{AbstractDAO, DAOHelpers}
import io.towerstreet.slick.models.generated.attacksimulator.Model.{ClientRequest, NetworkSegment, SimulationOutcomeId}
import io.towerstreet.slick.models.generated.attacksimulator.Tables
import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NetworkSegmentDAO @Inject()(val dbConfigProvider: DatabaseConfigProvider)
                                 (implicit ec: ExecutionContext)
  extends AbstractDAO
    with DAOHelpers
{
  import api._
  import implicits._

  def insertWithRequest(segment: NetworkSegment, request: ClientRequest): Future[NetworkSegment] = {
    val q = for {
      inserted <- Tables.NetworkSegmentTableInsert += segment
      _ <- Tables.ClientRequestTableInsert += request
    } yield inserted
    db.run(q)
  }

  def findClientSegments(outcomeId: SimulationOutcomeId, request: ClientRequest): Future[Seq[(InetString, Int)]] = {
    val q = for {
      customerId <- (
        for {
          o <- Tables.SimulationOutcomeTable if o.id === outcomeId
          s <- Tables.SimulationTable if o.simulationId === s.id
          u <- Tables.UserTable if s.userId === u.id
        } yield u.customerId
      )
        .take(1)
        .result
        .mapSingleResult()

      segments <- (
        for {
          ns <- Tables.NetworkSegmentTable
          o <- Tables.SimulationOutcomeTable if ns.simulationOutcomeId === o.id
          s <- Tables.SimulationTable if o.simulationId === s.id
          u <- Tables.UserTable if s.userId === u.id && u.customerId === customerId
        } yield ns
      )
        .groupBy(ns => (ns.subnetIp, ns.subnetPrefix))
        .map(t => t._1)
        .result

      _ <- Tables.ClientRequestTableInsert += request
    } yield segments

    db.run(q.transactionally)
  }
}
