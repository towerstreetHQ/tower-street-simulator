package io.towerstreet.attacksimulator.helpers

import play.api.libs.json.{JsObject, JsValue}

trait JsonHelpers {
  protected def mergeObjects(objects: Option[JsValue]*): Option[JsObject] = {
    objects
      .flatMap{
        case Some(p: JsObject) => Some(p)
        case _ => None
      }
      .reduceLeftOption(_ ++ _)
  }
}
