package io.towerstreet.attacksimulator.actors

import akka.actor.{Actor, ActorRef, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.pattern.pipe
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString
import com.google.inject.name.Named
import io.towerstreet.attacksimulator.models.UrlTest.UrlTaskParameters
import io.towerstreet.attacksimulator.services.UrlTestService.UrlTestRecord
import io.towerstreet.logging.Logging
import io.towerstreet.slick.models.generated.attacksimulator.Model.{Task, TaskId}
import javax.inject.Inject
import play.api.libs.json.JsValue

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object UrlTestActor {
  val DefaultImagePath = "favicon.ico"

  def props(urlTestServiceActor: ActorRef)(implicit ec: ExecutionContext): Props = Props(new UrlTestActor(urlTestServiceActor))

  case class TestTasks(tasks: Seq[Task])
}

class UrlTestActor @Inject()(@Named("urlTestServiceActor") urlTestServiceActor: ActorRef)
  (implicit ec: ExecutionContext) extends Actor with Logging {

  import UrlTestActor._

  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  override def receive: Receive = {
    case TestTasks(tasks) =>
      logger.debug(s"Received tasks, $tasks")
      tasks.flatMap(t => t.parameters.map(_ -> t.id))
        .flatMap { case (params, id) => getUri(params).map(_ -> id) }
        .foreach { case (uri, id) => checkUri(id, uri) pipeTo urlTestServiceActor }
  }

  def checkUri(taskId: TaskId, uri: Uri): Future[UrlTestRecord] = {
    val start = System.currentTimeMillis()
    logger.debug(s"Retrieving task ID: $taskId - URI: $uri")
    Http(context.system).singleRequest(HttpRequest(uri = uri))
        .flatMap {
          case HttpResponse(sc, _, entity, _) if sc.isSuccess() =>
            entity.dataBytes.runFold(ByteString(""))(_ ++ _)
                .map(content => UrlTestRecord(taskId, Some(sc.intValue()), Some(content.toArray),
                  Some(java.time.Duration.ofMillis(System.currentTimeMillis() - start))
                ))
                .recover { case throwable: Throwable =>
                    logger.warn(s"Unable to retrieve content of task ID $taskId", throwable)
                    UrlTestRecord(taskId, Some(sc.intValue()), None,
                      Some(java.time.Duration.ofMillis(System.currentTimeMillis() - start)))
                }
          case HttpResponse(sc, _, _, _) =>
            logger.warn(s"Check of task ID: $taskId ended with status code $sc")
            Future.successful(UrlTestRecord(taskId, Some(sc.intValue()), None,
              Some(java.time.Duration.ofMillis(System.currentTimeMillis() - start))))
        }.recover { case throwable: Throwable =>
            logger.warn(s"Unable to check task ID: $taskId - uri: $uri", throwable)
            UrlTestRecord(taskId, None, None, None)
        }
  }

  def getUri(jsValue: JsValue): Option[Uri] = {
    jsValue.asOpt[UrlTaskParameters].flatMap { params =>
      if (params.includeInUrlTesting) {
        val imagePath = params.imagePath.getOrElse(DefaultImagePath)

        // Shall we include "/" character between base and path, or it is already there?
        val pathSeparator = if (params.url.endsWith("/") || imagePath.startsWith("/")) "" else "/"
        // Does path contain query parameters with "?" or we are free to use it to separate cache invalidate parameter?
        val dateSeparator = if (imagePath.contains("?")) "&" else "?"

        val uri = s"${params.url}$pathSeparator$imagePath$dateSeparator${System.currentTimeMillis()}"
        Try(Uri(uri)).toOption
      } else {
        // Task should be excluded due to its settings
        // Typically its measuring endpoints (i.e. max timeout), baseline tests, or non existing domains
        logger.info(s"Excluded url: ${params.url}/${params.imagePath.getOrElse(DefaultImagePath)}")
        None
      }
    }
  }

}
