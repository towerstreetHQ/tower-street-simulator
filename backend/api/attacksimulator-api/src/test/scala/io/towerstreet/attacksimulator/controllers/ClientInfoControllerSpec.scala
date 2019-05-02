package io.towerstreet.attacksimulator.controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.libs.json.{JsNull, JsString}
import play.api.test.Helpers._
import play.api.test._

class ClientInfoControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

  "xForwardedFor GET" should {
    "return IP for provided IPv4 X-Forwarded-For header" in {
      val request = FakeRequest(GET, "/client-info/x-forwarded-for").withHeaders(X_FORWARDED_FOR -> "127.0.0.1")
      val response = route(app, request).get
      status(response) mustBe OK
      contentType(response) mustBe Some("application/json")
      contentAsJson(response) mustBe JsString("127.0.0.1")
    }
    "return IP for provided IPv4 compatible IPv6 X-Forwarded-For header" in {
      val request = FakeRequest(GET, "/client-info/x-forwarded-for").withHeaders(X_FORWARDED_FOR -> "::ffff:127.0.0.1")
      val response = route(app, request).get
      status(response) mustBe OK
      contentType(response) mustBe Some("application/json")
      contentAsJson(response) mustBe JsString("127.0.0.1")
    }
    "return null for provided IPv6 X-Forwarded-For header" in {
      val request = FakeRequest(GET, "/client-info/x-forwarded-for").withHeaders(X_FORWARDED_FOR -> "1080:0:0:0:8:800:200C:417A")
      val response = route(app, request).get
      status(response) mustBe OK
      contentType(response) mustBe Some("application/json")
      contentAsJson(response) mustBe JsNull
    }
    "return null for non-IP X-Forwarded-For header" in {
      val request = FakeRequest(GET, "/client-info/x-forwarded-for").withHeaders(X_FORWARDED_FOR -> "tower-street")
      val response = route(app, request).get
      status(response) mustBe OK
      contentType(response) mustBe Some("application/json")
      contentAsJson(response) mustBe JsNull
    }
    "return null for missing X-Forwarded-For header" in {
      val request = FakeRequest(GET, "/client-info/x-forwarded-for")
      val response = route(app, request).get
      status(response) mustBe OK
      contentType(response) mustBe Some("application/json")
      contentAsJson(response) mustBe JsNull
    }
  }

}
