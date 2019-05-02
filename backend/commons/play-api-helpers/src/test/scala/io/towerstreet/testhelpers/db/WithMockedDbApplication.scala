package io.towerstreet.testhelpers.db

import acolyte.jdbc.StatementHandler
import io.towerstreet.testhelpers.WithApplicationSpec

/**
  * Mixin trait to add supper for mocked database for specs with juice applications.
  */
trait WithMockedDbApplication extends WithApplicationSpec {
  val handler: StatementHandler

  private val id = System.identityHashCode(this).toString
  acolyte.jdbc.Driver.register(id, handler)

  private val dbConfig = Map(
    "slick.dbs.default.db.driver" -> "acolyte.jdbc.Driver",
    "slick.dbs.default.db.url" -> "jdbc:acolyte:test?handler=%s".format(id)
  )

  protected override def getConfig: Map[String, Any] = super.getConfig ++ dbConfig
}

object WithMockedDbApplication {

  /**
    * Helper abstract class which allows to pass handler and custom config as constructor parameters.
    * Simplifies writing test cases:
    *
    * "my test" should {
    *   "do test with mocked database connection" in new WithMockedDbApplicationSpec(handler) {
    *     ...
    *   }
    * }
    */
  abstract class WithMockedDbApplicationSpec(val handler: StatementHandler,
                                             override val config: Map[String, Any] = Map.empty
                                            )
    extends WithMockedDbApplication
}
