package io.towerstreet.dao

import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DbRunDAO @Inject()(val dbConfigProvider: DatabaseConfigProvider)
                        (implicit ec: ExecutionContext)
  extends AbstractDAO
{
  import api._

  def dbRun[R](q: DBIO[R]): Future[R] = {
    db.run(q)
  }

  def dbRunTransactionally[R](q: DBIO[R]): Future[R] = {
    db.run(q.transactionally)
  }
}
