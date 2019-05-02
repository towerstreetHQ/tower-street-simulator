package io.towerstreet.attacksimulator.services

import java.time.LocalDateTime

import io.towerstreet.attacksimulator.dao.UrlTestDAO
import io.towerstreet.attacksimulator.services.UrlTestService.UrlTestRecord
import io.towerstreet.logging.Logging
import io.towerstreet.slick.models.generated.attacksimulator.Model.{TaskId, UrlTest, UrlTestId}
import com.google.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UrlTestService @Inject()(val dbConfigProvider: DatabaseConfigProvider,
                               urlTestDAO: UrlTestDAO
                              )(implicit ec: ExecutionContext)
  extends Logging
{

  def storeUrlTest(record: UrlTestRecord): Future[UrlTest] = {
    val u = UrlTest(
      id = UrlTestId(0),
      clientRequest = None,
      taskId = record.taskId,
      size = record.urlContent.map(_.length.toLong).getOrElse(0l),
      duration = record.duration,
      createdAt = LocalDateTime.now(),
      status = record.status
    )

    urlTestDAO.createUrlTest(u)
  }

}

object UrlTestService {

  case class UrlTestRecord(taskId: TaskId,
                           status: Option[Int] = None,
                           urlContent: Option[Array[Byte]] = None,
                           duration: Option[java.time.Duration] = None)

}