package io.towerstreet.services.helpers

import slick.dbio.DBIO

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

trait DBIOUtils {

  implicit class DBIOSyntax[R](self: DBIO[R]) {
    def transformError(f: PartialFunction[Throwable, Throwable])(implicit ec: ExecutionContext): DBIO[R] = {
      self.cleanUp({
        case Some(e) =>
          DBIO.failed(f.applyOrElse(e, { _: Throwable => e }))

        case None =>
          DBIO.successful(None)
      }, false)
    }

    def resolveError(f: PartialFunction[Throwable, DBIO[R]])(implicit ec: ExecutionContext): DBIO[R] = {
      self.cleanUp({
        case Some(e) =>
          f.applyOrElse(e, { _: Throwable => DBIO.failed(e) })

        case None =>
          DBIO.successful(None)
      }, false)
    }

  }

  implicit class DBIOSeqSyntax[R](self: DBIO[Seq[R]]) {
    /**
      * Applies given function to each item in DBIO sequence. Resulting DBIO contains flattened sequence of
      * target types or FIRST failure in case that some map function returned DBIO failure.
      */
    def flatMapSeq[R2](f: R => DBIO[R2])(implicit ec: ExecutionContext): DBIO[Seq[R2]] = {
      self.flatMap(_.foldLeft[DBIO[Seq[R2]]](DBIO.successful(Seq.empty)) {
        (a, item) =>
          for {
            accumulator <- a
            mapped <- f(item)
          } yield accumulator :+ mapped
        }
      )
    }
  }
}

object DBIOUtils {
  def fromTry[R](t: Try[R]): DBIO[R] = {
    t match {
      case Success(r) => DBIO.successful(r)
      case Failure(e) => DBIO.failed(e)
    }
  }

  def fromEither[E <: Throwable, R](e: Either[E, R]): DBIO[R] = {
    e match {
      case Right(v) => DBIO.successful(v)
      case Left(e) => DBIO.failed(e)
    }
  }

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
}
