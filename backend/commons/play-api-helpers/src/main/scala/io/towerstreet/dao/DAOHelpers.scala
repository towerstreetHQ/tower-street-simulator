package io.towerstreet.dao

import io.towerstreet.exceptions.TowerstreetDAOException.{DuplicateKeysException, EntityNotFoundException}
import slick.lifted.AbstractTable

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag


trait DAOHelpers {
  this : AbstractDAO =>

  import api._

  def mapSingleResult[T](results: Seq[T])(implicit classTag: ClassTag[T]): Future[T] = {
    if (results.nonEmpty) {
      Future.successful(results.head)
    } else {
      Future.failed(EntityNotFoundException[T])
    }
  }

  def checkDuplicate[E <: AbstractTable[_]](table: TableQuery[E], filter: E => Rep[Boolean], keyCol: E => Rep[String])
                                           (implicit executionContext: ExecutionContext) = {
    for {
      existing <- table.filter(t => filter(t)).map(t => keyCol(t)).result
      _ <- maybeAction(existing.nonEmpty) {
        DBIO.failed(DuplicateKeysException(existing))
      }
    } yield ()
  }

  implicit class SeqDBIOSyntax[T](self: DBIO[Seq[T]])
                                 (implicit executionContext: ExecutionContext, classTag: ClassTag[T])
  {
    def mapSingleResult(): DBIO[T] = {
      self.flatMap{ results =>
        if (results.nonEmpty) {
          DBIO.successful(results.head)
        } else {
          DBIO.failed(EntityNotFoundException[T])
        }
      }
    }

    def mapItems[R](func: T => R): DBIO[Seq[R]] = {
      self.map(_.map(func))
    }
  }
}
