package io.towerstreet.attacksimulator.actors

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import io.towerstreet.attacksimulator.services.UrlTestService.UrlTestRecord
import io.towerstreet.slick.models.generated.attacksimulator.Model.{RunnerId, Task, TaskId}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import play.api.libs.json.{JsBoolean, JsObject, JsString, Json}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

class UrlTestActorTest extends TestKit(ActorSystem("testSystem")) with ImplicitSender with WordSpecLike
  with Matchers with BeforeAndAfterAll {

  implicit val ec: ExecutionContext = ExecutionContext.global

  override def afterAll: Unit = {
    TestKit. shutdownActorSystem(system)
  }

  "An UrlTestActor actor" must {

    "Check favicon of google and facebook" in {

      def expect: PartialFunction[Any, UrlTestRecord] = {
        case u@UrlTestRecord(id, Some(200), Some(content), Some(duration))
          if id == TaskId(1) && content.length > 1000l && duration.getNano > 1000l => u
        case u@UrlTestRecord(id, Some(200), Some(content), Some(duration))
          if id == TaskId(3) && content.length > 1000l && duration.getNano > 1000l => u
        case u@UrlTestRecord(id, None, None, None) if id == TaskId(4) => u
        case u@UrlTestRecord(id, None, None, None) if id == TaskId(5) => u
        case u@UrlTestRecord(id, Some(200), Some(content), Some(duration))
          if id == TaskId(6) && content.length > 1000l && duration.getNano > 1000l => u
        case u@UrlTestRecord(id, Some(200), Some(content), Some(duration))
          if id == TaskId(7) && content.length > 1000l && duration.getNano > 1000l => u
      }


      val probe = TestProbe()
      val urlTestActor = system.actorOf(UrlTestActor.props(probe.ref))
      urlTestActor ! UrlTestActor.TestTasks(
        Seq(
          // Valid Google url
          Task(TaskId(1), RunnerId(1), "test", Some(JsObject(Map("url" -> JsString("http://www.google.com"))))),
          // Invalid JSON key
          Task(TaskId(2), RunnerId(1), "test", Some(JsObject(Map("noturl" -> JsString("http://www.google.com"))))),
          // Facebook via HTTPS with backslash at the end of URI
          Task(TaskId(3), RunnerId(1), "test", Some(JsObject(Map("url" -> JsString("https://www.facebook.com/"))))),
          // Invalid url value
          Task(TaskId(4), RunnerId(1), "test", Some(JsObject(Map("url" -> JsString("123231512351235"))))),
          // Non-registered url
          Task(TaskId(5), RunnerId(1), "test", Some(JsObject(Map("url" -> JsString("http://www.123231512351235.com/"))))),
          // Custom image path
          Task(TaskId(6), RunnerId(1), "test", Some(JsObject(Map(
            "url" -> JsString("http://www.google.com"),
            "imagePath" -> JsString("images/branding/googlelogo/1x/googlelogo_color_272x92dp.png")
          )))),
          // Custom image path with get param
          Task(TaskId(7), RunnerId(1), "test", Some(JsObject(Map(
            "url" -> JsString("http://www.google.com"),
            "imagePath" -> JsString("images/branding/googlelogo/1x/googlelogo_color_272x92dp.png?p=1")
          )))),
          // Excluded test
          Task(TaskId(8), RunnerId(1), "test", Some(JsObject(Map(
            "url" -> JsString(""),
            "includeInUrlTesting" -> JsBoolean(false)
          ))))
        )
      )
      // As the number of cases in expect method
      probe.expectMsgPF[UrlTestRecord](10 seconds)(expect)
      probe.expectMsgPF[UrlTestRecord](10 seconds)(expect)
      probe.expectMsgPF[UrlTestRecord](10 seconds)(expect)
      probe.expectMsgPF[UrlTestRecord](10 seconds)(expect)
      probe.expectMsgPF[UrlTestRecord](10 seconds)(expect)
      probe.expectMsgPF[UrlTestRecord](10 seconds)(expect)

    }

  }

}
