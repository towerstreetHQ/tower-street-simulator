package io.towerstreet.attacksimulator.controllers

import java.util.UUID

import akka.util.ByteString
import io.towerstreet.attacksimulator.services.TestSinkService
import io.towerstreet.controllers.ControllerHelpers
import io.towerstreet.logging.Logging
import io.towerstreet.slick.models.generated.attacksimulator.Model.TaskId
import javax.inject._
import play.api.libs.streams._
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.{AbstractController, ControllerComponents}
import play.core.parsers.Multipart.FileInfo

import scala.concurrent.ExecutionContext

@Singleton
class TestSinkController @Inject()(cc: ControllerComponents,
                                   testSinkService: TestSinkService
                                  )(implicit ec: ExecutionContext)
  extends AbstractController(cc)
    with ControllerHelpers
    with Logging
{
  type FilePartHandler[A] = FileInfo => Accumulator[ByteString, FilePart[A]]

  def postFileSink(name: String, outcomeToken: UUID, testId: Int) = {
    Action(parse.raw).async { implicit request =>
      testSinkService.fileSink(name, outcomeToken, TaskId(testId), request).toJson()
    }
  }
}
