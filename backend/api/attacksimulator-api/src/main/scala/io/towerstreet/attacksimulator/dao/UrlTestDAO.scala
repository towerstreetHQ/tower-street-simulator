package io.towerstreet.attacksimulator.dao

import java.time.LocalDateTime

import io.towerstreet.dao.{AbstractDAO, DAOHelpers}
import io.towerstreet.slick.models.generated.attacksimulator.Model.{TaskId, UrlTest}
import io.towerstreet.slick.models.generated.attacksimulator.Tables
import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UrlTestDAO @Inject()(val dbConfigProvider: DatabaseConfigProvider)
                          (implicit ec: ExecutionContext)
  extends AbstractDAO
    with DAOHelpers
{
  import api._
  import implicits._

  def createUrlTest(urlTest: UrlTest): Future[UrlTest] = {
    db.run((Tables.UrlTestTableInsert += urlTest).transactionally)
  }

  def findResultForTask(taskId: TaskId, createdThreshold: LocalDateTime): Future[Option[UrlTest]] = {
    val q = Tables.UrlTestTable
      .filter(t => t.taskId === taskId && t.createdAt > createdThreshold)
      .sortBy(_.createdAt.desc)
      .take(1)
      .result
      .headOption

    db.run(q)
  }

}
