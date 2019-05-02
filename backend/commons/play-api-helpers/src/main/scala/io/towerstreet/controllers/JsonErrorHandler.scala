package io.towerstreet.controllers

import java.io.{PrintWriter, StringWriter}

import io.towerstreet.exceptions.TowerStreetApiException
import io.towerstreet.exceptions.{BadRequestException, ForbiddenException, NotFoundException}
import javax.inject._
import play.api._
import play.api.http.DefaultHttpErrorHandler
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc._
import play.api.routing.Router

import scala.concurrent._

@Singleton
class JsonErrorHandler @Inject() (env: Environment,
                                  config: Configuration,
                                  sourceMapper: OptionalSourceMapper,
                                  router: Provider[Router]
                                 )
  extends DefaultHttpErrorHandler(env, config, sourceMapper, router)
{

  private def jsonErrorHandler(message: String, status: Status, defaultMessage: Option[String] = None) = {
    Future.successful(status(
      Json.obj(
        "status" ->"error",
        "message" -> Some(message).filter(_.nonEmpty).orElse(defaultMessage)
      )
    ))
  }

  private def onApiException(e: TowerStreetApiException) = {
    Future.successful(Status(e.status)(
      Json.obj(
        "status" ->"error",
        "message" -> Some(e.message),
        "payload" -> e.payload
      )
    ))
  }

  override protected def onBadRequest(request: RequestHeader, message: String): Future[Result] =
    jsonErrorHandler(message, BadRequest, Some("Bad user request, check request body"))

  override protected def onNotFound(request: RequestHeader, message: String): Future[Result] =
    jsonErrorHandler(message, NotFound, Some("Resource not found: " + request.uri))


  override protected def onForbidden(request: RequestHeader, message: String): Future[Result] =
    jsonErrorHandler(message, Forbidden, Some("Forbidden"))

  override protected def onOtherClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] =
    jsonErrorHandler(message, Results.Status(statusCode))

  // Root error handler to catch special exceptions and prevent play logging
  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    exception match {
      case _: NotFoundException => onNotFound(request, "")
      case _: BadRequestException => onBadRequest(request, "")
      case _: ForbiddenException => onForbidden(request, "")
      case e: TowerStreetApiException => onApiException(e)

      case _ => super.onServerError(request, exception)
    }
  }

  override protected def onProdServerError(request: RequestHeader, exception: UsefulException): Future[Result] = {
    Future.successful(InternalServerError(
      Json.obj(
        "status" -> "error"
      )
    ))
  }

  override protected def onDevServerError(request: RequestHeader, exception: UsefulException): Future[Result] = {
    val rootException = Option(exception.getCause).getOrElse(exception)
    Future.successful(InternalServerError(
      Json.obj(
        "status" ->"error",
        "message" -> rootException.getMessage,
        "exception" -> printStackTrace(rootException)
      )
    ))
  }

  private def printStackTrace(exception: Throwable) = {
    val sw = new StringWriter
    val pw = new PrintWriter(sw)
    exception.printStackTrace(pw)

    var ex = exception.getCause
    while (ex != null) {
      sw.append("\nCaused by: ")
      ex.printStackTrace(pw)
      ex = ex.getCause
    }

    sw.toString
  }
}
