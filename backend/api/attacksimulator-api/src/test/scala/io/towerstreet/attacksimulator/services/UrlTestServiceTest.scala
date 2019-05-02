package io.towerstreet.attacksimulator.services

import acolyte.jdbc.{AcolyteDSL, StatementHandler}
import io.towerstreet.attacksimulator.services.UrlTestService.UrlTestRecord
import io.towerstreet.slick.models.generated.attacksimulator.Model.TaskId
import io.towerstreet.testhelpers.WithApplicationSpec.WithService
import io.towerstreet.testhelpers.db.ExecutorHandlers.{Handler, Param, ~}
import io.towerstreet.testhelpers.db.ResultHelpers.IntKeyRowList
import io.towerstreet.testhelpers.db.WithMockedDbApplication
import org.scalatestplus.play.PlaySpec
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

import scala.collection.mutable

class UrlTestServiceTest extends PlaySpec with FutureAwaits with DefaultAwaitTimeout {

  class WithSpecApplication extends WithMockedDbApplication with WithService[UrlTestService] {

    val testResults = mutable.ArrayBuffer.empty[Seq[Any]]

    override lazy val handler: StatementHandler = Handler("^select ") {
      case ~(Param("""^insert into "attacksimulator"."url_test""""), v@_*) =>
        testResults += v
        AcolyteDSL.updateResult(1, IntKeyRowList.append(1))
    }
  }

  "UrlTestService" should {
    "storeUrlTest" should {
      "persist single UrlTestRecord" in new WithSpecApplication {
        val ba = "Hello".toArray.map(_.toByte)
        val urlTestRecord = UrlTestRecord(TaskId(0), Some(200), Some(ba), Some(java.time.Duration.ofSeconds(1l)))
        val r = await(service.storeUrlTest(urlTestRecord))
        r.id must be (TaskId(1))
        r.clientRequest must be (None)
        r.size must be (5)
        r.status must be (Some(200))

        testResults must have size 1l
      }
    }
  }

}
