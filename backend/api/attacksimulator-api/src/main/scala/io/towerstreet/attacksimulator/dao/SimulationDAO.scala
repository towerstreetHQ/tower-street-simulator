package io.towerstreet.attacksimulator.dao

import java.util.UUID

import io.towerstreet.dao.{AbstractDAO, DAOHelpers}
import io.towerstreet.slick.models.generated.attacksimulator.Model._
import io.towerstreet.slick.models.generated.attacksimulator.Tables
import io.towerstreet.slick.models.generated.public.Model.CustomerId
import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SimulationDAO @Inject()(val dbConfigProvider: DatabaseConfigProvider)
                             (implicit ec: ExecutionContext)
  extends AbstractDAO
    with DAOHelpers
{
  import api._
  import implicits._

  def getSimulationDefinitionByToken(token: UUID): Future[(Simulation, Seq[TaskForSimulation])] = {
    val q = for {
      simulation <- Tables.SimulationTable.filter(_.token === token)
        .result
        .mapSingleResult()

      config <- Tables.TaskForSimulationTable
        .filter(_.templateId === simulation.templateId)
        .sortBy(_.position)
        .result
    } yield (simulation, config)

    db.run(q.transactionally)
  }
}
