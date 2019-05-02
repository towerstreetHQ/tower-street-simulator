package io.towerstreet.testhelpers.db

import acolyte.jdbc.RowLists.rowList1
import acolyte.jdbc.{AcolyteDSL, RowLists, UpdateResult}

import scala.util.Try

object ResultHelpers {
  lazy val IntKeyRowList =
    rowList1(classOf[Int]).withLabel(1, "id")

  def intKeysResult(start: Int, stop: Int): UpdateResult = {
    var rowList = IntKeyRowList
    for (i <- start to stop) {
      rowList = rowList.append(i)
    }

    AcolyteDSL.updateResult(rowList.getRows.size(), rowList)
  }

  def intKeyResult(key: Int): UpdateResult = {
    AcolyteDSL.updateResult(1, IntKeyRowList.append(key))
  }

  def generateKeys(count: Int, start: Int) = {
    val keys: Array[Integer] = start.until(start + count).map(i => new Integer(i)).toArray
    AcolyteDSL.updateResult(
      count = count,
      keys = RowLists.intList(keys: _*)
    )
  }

  def parameterKeyResult(param: Any): UpdateResult = {
    val keyOpt = for {
      last <- param match {
        case str: String  => str.split("-").lastOption
        case _            => None
      }

      key <- Try(last.stripPrefix("0").toInt).toOption
    } yield IntKeyRowList.append(key)

    keyOpt.fold(AcolyteDSL.updateResult(0, IntKeyRowList))(AcolyteDSL.updateResult(1, _))
  }
}
