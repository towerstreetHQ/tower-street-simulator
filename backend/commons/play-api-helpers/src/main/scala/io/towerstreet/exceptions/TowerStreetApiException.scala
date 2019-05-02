package io.towerstreet.exceptions

import play.api.http.Status
import play.api.libs.json.JsObject

trait TowerStreetApiException extends TowerstreetException {
  val status: Int = Status.BAD_REQUEST
  def payload: Option[JsObject] = None
}
