package io.towerstreet.controllers

import play.api.libs.json.{JsError, Json, Reads, Writes}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

trait ControllerHelpers {

  this: AbstractController =>

  /**
    * This helper parses and validates JSON using the implicit json readers
    * above, returning errors if the parsed json fails validation.
    */
  def validateJson[T : Reads](implicit executionContext: ExecutionContext): BodyParser[T] = parse.json.validate(
    _.validate[T].asEither.left.map(e => BadRequest(JsError.toJson(e)))
  )

  def jsonResponse[T](response: T, status: Status = Ok)(implicit writer: Writes[T]): Result = {
    status(Json.toJson(response))
  }

  implicit class FutureSyntax[T](self: Future[T])
                                (implicit executionContext: ExecutionContext)
  {
    def toJson(status: Status = Ok)(implicit writer: Writes[T]): Future[Result] = {
      self.map(jsonResponse(_, status))
    }

    def noContent(result: => Result = NoContent): Future[Result] = {
      self.map(_ => result)
    }
  }
}
