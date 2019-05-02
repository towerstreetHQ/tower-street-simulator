package io.towerstreet

package object exceptions {
  trait TowerstreetException extends Exception
  {
    val message: String
    val cause: Option[Throwable] = None

    override def getMessage: String = message
    override def getCause: Throwable = cause.orNull
  }

  case class BadRequestException(message: String) extends TowerstreetException
  case class NotFoundException(message: String) extends TowerstreetException
  case class ForbiddenException(message: String) extends TowerstreetException
}
