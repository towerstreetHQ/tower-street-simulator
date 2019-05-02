package io.towerstreet.attacksimulator.controllers

import io.towerstreet.attacksimulator.models.Version
import io.towerstreet.controllers.ControllerHelpers
import javax.inject._
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, Request}

import scala.concurrent.ExecutionContext

@Singleton
class IndexController @Inject()(cc: ControllerComponents)(implicit ec: ExecutionContext)
  extends AbstractController(cc)
    with ControllerHelpers
{
  def version() = Action { implicit request: Request[AnyContent] =>
    jsonResponse(Version.BuildInfoVersion)
  }
}


