package io.towerstreet.services.helpers

import io.towerstreet.dao.DbRunDAO
import io.towerstreet.logging.Logging
import javax.inject.Inject
import slick.dbio.DBIO

import scala.concurrent.Future

/**
  * Mixin trait to propagate DB run to services. Usable when service wants to do some work encapsulated in
  * single transaction. Thrown exceptions cases DB rollback.
  */
trait WithDbTransaction {
  this : Logging =>

  @Inject() private var _dbRunDao: DbRunDAO = _

  object DbTransaction {
    def apply[R](q: DBIO[R]): Future[R] = {
      _dbRunDao.dbRunTransactionally(q)
    }
  }

  object DbRun {
    def apply[R](q: DBIO[R]): Future[R] = {
      _dbRunDao.dbRun(q)
    }
  }
}
