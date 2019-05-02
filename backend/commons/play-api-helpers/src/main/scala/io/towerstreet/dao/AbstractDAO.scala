package io.towerstreet.dao

import io.towerstreet.slick.dao.SlickDAO
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

abstract class AbstractDAO extends SlickDAO {
  val dbConfigProvider: DatabaseConfigProvider
  protected val dbConfig = dbConfigProvider.get[JdbcProfile]
}
