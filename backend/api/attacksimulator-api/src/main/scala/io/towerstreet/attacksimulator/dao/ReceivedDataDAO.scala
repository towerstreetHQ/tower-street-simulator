package io.towerstreet.attacksimulator.dao

import io.towerstreet.dao.{AbstractDAO, DAOHelpers}
import io.towerstreet.slick.models.generated.attacksimulator.Model.{ClientRequest, ReceivedData}
import io.towerstreet.slick.models.generated.attacksimulator.Tables
import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReceivedDataDAO @Inject()(val dbConfigProvider: DatabaseConfigProvider)
                                (implicit ec: ExecutionContext)
  extends AbstractDAO
    with DAOHelpers
{
  def insertWithRequest(data: ReceivedData, request: ClientRequest): Future[ReceivedData] = {
    val q = for {
      inserted <- Tables.ReceivedDataTableInsert += data
      _ <- Tables.ClientRequestTableInsert += request.copy(receivedDataId = Some(inserted.id))
    } yield inserted
    db.run(q)
  }
}
