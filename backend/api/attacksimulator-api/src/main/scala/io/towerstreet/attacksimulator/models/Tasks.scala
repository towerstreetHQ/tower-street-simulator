package io.towerstreet.attacksimulator.models

import com.github.vitalsoftware.macros.json
import io.towerstreet.json.{JsonValidators, TagFormat}
import io.towerstreet.slick.models.generated.attacksimulator.Model.{Runner, Task, TaskId}
import play.api.libs.functional.syntax._
import play.api.libs.json._

object Tasks
  extends JsonValidators
    with TagFormat
{

  val TaskKeyMaxLength = 32
  val TaskKeyReader: Reads[String] = nonEmptyString(TaskKeyMaxLength)

  case class CreateTaskRequest(runnerName: String,
                               taskKey: String,
                               parameters: Option[JsValue]
                              )
  object CreateTaskRequest {
    implicit val format: Format[CreateTaskRequest] =
      Format(
        ((JsPath \ "runnerName").read[String](Runners.RunnerNameReader) and
          (JsPath \ "taskKey").read[String](TaskKeyReader) and
          (JsPath \ "parameters").readNullable[JsValue]
          )(CreateTaskRequest.apply _),
        Json.writes[CreateTaskRequest]
      )
  }

  @json
  case class CreateTasksRequest(tasks: Seq[CreateTaskRequest])

  @json
  case class TaskResponse(id: TaskId,
                          runnerName: String,
                          taskKey: String,
                          parameters: Option[JsValue])
  object TaskResponse {
    def apply(task: Task, runner: Runner): TaskResponse = {
      TaskResponse(task.id, runner.runnerName, task.taskKey, task.parameters)
    }

    def toTasks(tasks: Seq[Task], runners: Seq[Runner]): Seq[TaskResponse] = {
      val nameMap = runners.map(r => r.id -> r).toMap
      tasks.map(t => TaskResponse(t, nameMap(t.runnerId)))
    }
  }

  case class TaskKeyWithParameters(taskKey: String,
                                   parameters: Option[JsValue])
  object TaskKeyWithParameters {
    implicit val format: Format[TaskKeyWithParameters] =
      Format(
        ((JsPath \ "taskKey").read[String](TaskKeyReader) and
          (JsPath \ "parameters").readNullable[JsValue]
          )(TaskKeyWithParameters.apply _),
        Json.writes[TaskKeyWithParameters]
      )
  }
}
