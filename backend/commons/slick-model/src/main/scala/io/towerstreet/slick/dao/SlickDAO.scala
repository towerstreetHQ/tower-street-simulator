package io.towerstreet.slick.dao

import io.towerstreet.slick.db.{ColumnMappers, TsPostgresProfile}
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext

trait SlickDAO {
  protected val dbConfig: DatabaseConfig[JdbcProfile]

  protected val profile: TsPostgresProfile.type = TsPostgresProfile
  protected val implicits: ColumnMappers.type = ColumnMappers
  protected val api: TsPostgresProfile.api.type = TsPostgresProfile.api
  protected lazy val db = dbConfig.db

  import api._

  def maybeAction[R](condition: Boolean)
                    (action: => DBIO[_ <: R],
                     alternative: => DBIO[_ <: Option[R]] = DBIO.successful(None))
                    (implicit executionContext: ExecutionContext): DBIO[Option[R]] = {
    if (condition) {
      action.map(r => Some(r))
    } else {
      alternative
    }
  }

  def maybeActionOpt[T, R](opt: Option[T])
                          (action: T => DBIO[R],
                           alternative: => DBIO[_ <: Option[R]] = DBIO.successful(None))
                          (implicit executionContext: ExecutionContext): DBIO[Option[R]] = {
    opt.fold[DBIO[Option[R]]](alternative){ value =>
      action(value).map(r => Some(r))
    }
  }
}
