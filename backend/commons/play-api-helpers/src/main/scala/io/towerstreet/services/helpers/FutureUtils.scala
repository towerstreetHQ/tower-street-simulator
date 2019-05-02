package io.towerstreet.services.helpers

import scala.concurrent.{ExecutionContext, Future}

object FutureUtils {

  /**
    * Perform future calculation bunch of parallel chunks which runs in sequence. Usable to avoid flooding execution
    * context with too many futures.
    *
    * Use case is while running too many DB operations at once. EC is not able to separate slick operations - they will
    * open connection and then becomes inactive so EC will run another future. This will cause that futures will
    * create too many connections and they will timeout.
    */
  def calculateSequential[T, R](seq: Seq[T], concurrency: Int = 1)
                               (f: T => Future[R])
                               (implicit ec: ExecutionContext): Future[Seq[R]] = {
    seq.grouped(concurrency).foldLeft(Future.successful(Seq.empty[R])) { (acc, partition) =>
      for {
        prev <- acc
        current <- Future.sequence(partition.map(f))
      } yield prev ++ current
    }
  }
}
