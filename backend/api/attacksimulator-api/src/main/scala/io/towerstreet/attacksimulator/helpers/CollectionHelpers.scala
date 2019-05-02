package io.towerstreet.attacksimulator.helpers

object CollectionHelpers {
  /**
    * Scans given collections for duplicate values. Returns list of all duplicated values.
    */
  def findDuplicities[T](seq: Seq[T]): Seq[T] = {
    seq.groupBy(t => t)
      .filter(_._2.size > 1)
      .keys
      .toSeq
  }
}
