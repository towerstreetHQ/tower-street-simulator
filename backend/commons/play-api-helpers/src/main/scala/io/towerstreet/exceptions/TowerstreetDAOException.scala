package io.towerstreet.exceptions

import scala.reflect.ClassTag

trait TowerstreetDAOException extends TowerstreetException

object TowerstreetDAOException {
  case class DuplicateKeysException(keys: Seq[String]) extends TowerstreetDAOException {
    override val message: String = "Insert to database failed due to violating of unique constraint"
  }

  class EntityNotFoundException[T](implicit classTag: ClassTag[T]) extends TowerstreetDAOException {
    override val message: String = s"Entity ${classTag.runtimeClass.getCanonicalName} hasn't been found in the database"
  }
  object EntityNotFoundException {
    def apply[T](implicit classTag: ClassTag[T]) = new EntityNotFoundException[T]
  }
}
