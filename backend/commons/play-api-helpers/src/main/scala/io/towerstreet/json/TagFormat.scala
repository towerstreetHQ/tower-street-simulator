package io.towerstreet.json

import play.api.libs.json._
import shapeless.tag
import shapeless.tag.@@

trait TagFormat {
  implicit def tagFormatInt[Class]: Format[Int @@ Class] = Format(
    {
      case JsNumber(value) => JsSuccess(tag[Class][Int](value.toIntExact))
      case _ => JsError("error.expected.jsnumber")
    },
    (o: Int @@ Class) => JsNumber(o)
  )

  implicit def tagFormatLong[Class]: Format[Long @@ Class] = Format(
    {
      case JsNumber(value) => JsSuccess(tag[Class][Long](value.toLongExact))
      case _ => JsError("error.expected.jsnumber")
    },
    (o: Long @@ Class) => JsNumber(o)
  )

  implicit def tagFormatString[Class]: Format[String @@ Class] = Format(
    {
      case JsString(value) => JsSuccess(tag[Class][String](value))
      case _ => JsError("error.expected.jsstring")
    },
    (o: String @@ Class) => JsString(o)
  )
}
