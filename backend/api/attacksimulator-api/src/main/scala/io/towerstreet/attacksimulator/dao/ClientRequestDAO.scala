package io.towerstreet.attacksimulator.dao

import io.towerstreet.dao.{AbstractDAO, DAOHelpers}
import io.towerstreet.slick.models.generated.attacksimulator.Model.ClientRequest
import io.towerstreet.slick.models.generated.attacksimulator.Tables
import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClientRequestDAO @Inject()(val dbConfigProvider: DatabaseConfigProvider)
                                (implicit ec: ExecutionContext)
  extends AbstractDAO
    with DAOHelpers
{
  import api._

  def insert(request: ClientRequest): Future[ClientRequest] = {
    db.run(Tables.ClientRequestTableInsert += request)
  }

  def insertQuery(request: ClientRequest): DBIO[ClientRequest] = {
    Tables.ClientRequestTableInsert += request
  }
}
