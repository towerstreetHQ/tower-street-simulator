package io.towerstreet.attacksimulator.controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.libs.json.JsString
import play.api.test.Helpers._
import play.api.test._

class IndexControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

  "IndexController GET" should {
    "return application version" in {
      val request = FakeRequest(GET, "/version")
      val version = route(app, request).get

      status(version) mustBe OK
      contentType(version) mustBe Some("application/json")
      (contentAsJson(version) \ "name").toOption mustBe Some(JsString("attacksimulator-api"))
    }
  }
}
