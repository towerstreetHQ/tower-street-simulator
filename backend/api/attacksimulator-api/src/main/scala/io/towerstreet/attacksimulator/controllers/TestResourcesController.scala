package io.towerstreet.attacksimulator.controllers

import akka.stream.scaladsl.StreamConverters
import io.towerstreet.attacksimulator.services.FileResourceService
import io.towerstreet.controllers.ControllerHelpers
import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, Request}

import scala.concurrent.ExecutionContext

@Singleton
class TestResourcesController @Inject()(cc: ControllerComponents,
                                        encodedFileService: FileResourceService
                                       )(implicit ec: ExecutionContext)
  extends AbstractController(cc)
    with ControllerHelpers
{
  def fileResource(fileKey: String) = Action { implicit request: Request[AnyContent] =>
    val stream = encodedFileService.getEncodedFile(fileKey)
    val dataContent = StreamConverters.fromInputStream(() => stream)

    Ok.chunked(dataContent)
  }
  def eicarResource(fileKey: String) = Action { implicit request: Request[AnyContent] =>
    val stream = encodedFileService.getFile(fileKey)
    val dataContent = StreamConverters.fromInputStream(() => stream)

    Ok.chunked(dataContent)
  }
}
