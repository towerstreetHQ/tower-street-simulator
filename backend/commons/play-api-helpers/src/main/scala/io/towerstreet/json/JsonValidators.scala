package io.towerstreet.json

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.{JsPath, JsonValidationError, Reads}

class JsonValidators {
  def nonEmptyString: Reads[String] =
    Reads.of[String].filter(JsonValidationError("error.string.nonEmpty"))(_.nonEmpty)

  def nonEmptyString(maxLen: Int): Reads[String] =
    nonEmptyString keepAnd maxLength[String](maxLen)

  implicit class JsPathSyntax(self: JsPath) {
    def readSeq[T](implicit r: Reads[T]): Reads[Seq[T]] = self.read[Seq[T]]
    def readSeqOpt[T](implicit r: Reads[T]): Reads[Seq[T]] = self.readWithDefault(Seq.empty[T])
  }
}
