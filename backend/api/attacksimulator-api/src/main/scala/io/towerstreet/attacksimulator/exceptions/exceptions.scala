package io.towerstreet.attacksimulator

import java.util.UUID

import io.towerstreet.exceptions.TowerStreetApiException
import io.towerstreet.json.TagFormat
import io.towerstreet.slick.models.generated.attacksimulator.Model.{SimulationOutcomeId, TaskId, UserId}
import io.towerstreet.slick.models.generated.public.Model.CustomerId
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}

package object exceptions extends TagFormat {

  case class MissingTasksException(missingKeys: Seq[String] = Seq(), missingIds: Seq[TaskId] = Seq()) extends TowerStreetApiException {
    val message = "Tasks with following keys haven't been found"
    override def payload = Some(JsObject(Seq(
      "missingKeys" -> Json.toJson(missingKeys),
      "missingIds" -> Json.toJson(missingIds)
    )))
  }
  case class TaskNotFoundException(key: String) extends TowerStreetApiException {
    val message = "Task with following key hasn't been found"
    override def payload = Some(JsObject(Seq(
      "key" -> Json.toJson(key)
    )))
  }
  case class TaskNotFoundByIdException(id: TaskId) extends TowerStreetApiException {
    val message = "Task with following id hasn't been found"
    override def payload = Some(JsObject(Seq(
      "id" -> Json.toJson(id)
    )))
  }
  case class SimulationNotFoundByTokenException(token: UUID) extends TowerStreetApiException {
    val message = "Simulation with provided token doesn't exist"
    override val status = Status.NOT_FOUND
    override def payload = Some(JsObject(Seq(
      "token" -> Json.toJson(token)
    )))
  }
  case class OutcomeNotFoundByTokenException(token: UUID) extends TowerStreetApiException {
    val message = "Simulation outcome with provided token doesn't exist"
    override val status = Status.NOT_FOUND
    override def payload = Some(JsObject(Seq(
      "token" -> Json.toJson(token)
    )))
  }
  case class OutcomeAlreadyFinishedException(token: UUID, outcomeId: SimulationOutcomeId) extends TowerStreetApiException {
    val message = "Simulation outcome with provided token has been already finished"
    override def payload = Some(JsObject(Seq(
      "token" -> Json.toJson(token)
    )))
  }
  case class MissingFileBodyException(size: Long, maxSize: Long) extends TowerStreetApiException {
    val message = "Error resolving body of received file"
    override def payload = Some(JsObject(Seq(
      "size" -> Json.toJson(size),
      "maxSize" -> Json.toJson(maxSize)
    )))
  }
}
