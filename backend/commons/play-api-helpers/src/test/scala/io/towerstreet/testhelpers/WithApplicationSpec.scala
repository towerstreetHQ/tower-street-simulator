package io.towerstreet.testhelpers

import io.towerstreet.dao.DbRunDAO
import play.api.Application
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.test.Injecting
import slick.dbio.DBIO

import scala.concurrent.Future

/**
  * Helper class to create applications with custom config per test case. This allows each test case to override
  * configuration to fulfil the test case.
  *
  * This pattern also allows to add custom mixins to add some functionality to test cases.
  *
  * Usage:
  * "my test" should {
  *   "do test with configuration" in new WithApplicationSpec(Map("uri" -> "jdbc:postgresql://127.0.0.1/db1")) {
  *     ...
  *   }
  *   "do test with another configuration" in new WithApplicationSpec(Map("uri" -> "jdbc:postgresql://127.0.0.1/db2")) {
  *     ...
  *   }
  * }
  *
  * One can create custom class to add common code for tests - inject more services, define constants, ...
  *
  * class WithSpecApplication() extends WithApplicationSpec {
  *   val myService = inject[MyService]
  *   val myDAO = inject[MyDAO]
  * }
  *
  * "my test" should {
  *   "do test with custom application" in new WithSpecApplication {
  *     myService.doJob()
  *     myDAO.getResults() must have size 1
  *   }
  * }
  */
abstract class WithApplicationSpec(val config: Map[String, Any] = Map.empty, val disableSchedulers: Boolean = true) extends Injecting
{
  protected def getConfig: Map[String, Any] = {
    config
  }

  protected def getInjectOverrides: Seq[GuiceableModule] = Seq()

  protected def getDisabledModules: Seq[Class[_]] = Seq()

  protected def getApplicationBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(getConfig)
      .overrides(getInjectOverrides:_*)
      .disable(getDisabledModules:_*)

  lazy val app: Application = getApplicationBuilder.build()
}

object WithApplicationSpec {
  /**
    * Mixin trait to add shortcut to inject service which will be tested in test cases.
    *
    * class WithSpecApplication() extends WithApplicationSpec
    *   with WithService[MyService]
    *
    * "my test" should {
    *   "do test with custom application" in new WithSpecApplication {
    *     service.doJob() must have size 1
    *   }
    * }
    */
  trait WithService[T] {
    this : Injecting =>

    private var instance: Option[T] = None

    def service(implicit tag: reflect.ClassTag[T]): T = instance.fold{
      val i = inject[T]
      instance = Some(i)
      i
    }(i => i)
  }

  trait WithDbRun {
    this : Injecting =>

    private var instance: Option[DbRunDAO] = None

    private def dao: DbRunDAO = instance.fold{
      val i = inject[DbRunDAO]
      instance = Some(i)
      i
    }(i => i)

    def dbRun[R](dbio: DBIO[R]): Future[R] = dao.dbRun(dbio)
  }
}


