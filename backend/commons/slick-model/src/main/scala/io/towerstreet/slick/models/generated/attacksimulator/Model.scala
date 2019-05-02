
package io.towerstreet.slick.models.generated.attacksimulator

import shapeless.tag.@@
import shapeless.tag
import slick.collection.heterogeneous._
import slick.collection.heterogeneous.syntax.HNil
import io.towerstreet.slick.models.generated._

object Model {

  /** Entity class storing rows of table CampaignVisitorSimulationTable
   *  @param visitorId Database column visitor_id SqlType(int4)
   *  @param customerId Database column customer_id SqlType(int4)
   *  @param simulationId Database column simulation_id SqlType(int4)
   *  @param simulationToken Database column simulation_token SqlType(uuid)
   *  @param templateId Database column template_id SqlType(int4)
   *  @param simulationOutcomeId Database column simulation_outcome_id SqlType(int4), Default(None)
   *  @param simulationOutcomeCreatedAt Database column simulation_outcome_created_at SqlType(timestamp), Default(None)
   *  @param simulationOutcomeFinishedAt Database column simulation_outcome_finished_at SqlType(timestamp), Default(None)
   *  @param simulationOutcomeLastPingAt Database column simulation_outcome_last_ping_at SqlType(timestamp), Default(None)
   *  @param scoringOutcomeId Database column scoring_outcome_id SqlType(int4), Default(None)
   *  @param scoringQueuedAt Database column scoring_queued_at SqlType(timestamp), Default(None)
   *  @param scoringFinishedAt Database column scoring_finished_at SqlType(timestamp), Default(None)
   *  @param simulationUserId Database column simulation_user_id SqlType(int4) */
  case class CampaignVisitorSimulation(visitorId: io.towerstreet.slick.models.generated.public.Model.CampaignVisitorId, customerId: io.towerstreet.slick.models.generated.public.Model.CustomerId, simulationId: io.towerstreet.slick.models.generated.attacksimulator.Model.SimulationId, simulationToken: java.util.UUID, templateId: io.towerstreet.slick.models.generated.attacksimulator.Model.SimulationTemplateId, simulationOutcomeId: Option[io.towerstreet.slick.models.generated.attacksimulator.Model.SimulationOutcomeId] = None, simulationOutcomeCreatedAt: Option[java.time.LocalDateTime] = None, simulationOutcomeFinishedAt: Option[java.time.LocalDateTime] = None, simulationOutcomeLastPingAt: Option[java.time.LocalDateTime] = None, scoringOutcomeId: Option[io.towerstreet.slick.models.generated.scoring.Model.ScoringOutcomeId] = None, scoringQueuedAt: Option[java.time.LocalDateTime] = None, scoringFinishedAt: Option[java.time.LocalDateTime] = None, simulationUserId: io.towerstreet.slick.models.generated.attacksimulator.Model.UserId)
  
  
  trait ClientRequestTag
  type ClientRequestId = Int @@ ClientRequestTag
  
  object ClientRequestId {
    def apply(id: Int): ClientRequestId = tag[ClientRequestTag][Int](id)
  }
             
  /** Entity class storing rows of table ClientRequestTable
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param inetAddr Database column inet_addr SqlType(inet), Length(2147483647,false)
   *  @param userAgent Database column user_agent SqlType(varchar), Default(None)
   *  @param requestedAt Database column requested_at SqlType(timestamp)
   *  @param resource Database column resource SqlType(varchar)
   *  @param responseStatus Database column response_status SqlType(varchar)
   *  @param simulationId Database column simulation_id SqlType(int4), Default(None)
   *  @param simulationOutcomeId Database column simulation_outcome_id SqlType(int4), Default(None)
   *  @param receivedDataId Database column received_data_id SqlType(int4), Default(None)
   *  @param token Database column token SqlType(uuid), Default(None) */
  case class ClientRequest(id: ClientRequestId, inetAddr: com.github.tminglei.slickpg.InetString, userAgent: Option[String] = None, requestedAt: java.time.LocalDateTime, resource: String, responseStatus: io.towerstreet.slick.models.attacksimulator.enums.ClientRequestStatus, simulationId: Option[SimulationId] = None, simulationOutcomeId: Option[SimulationOutcomeId] = None, receivedDataId: Option[ReceivedDataId] = None, token: Option[java.util.UUID] = None)
  
  
  trait NetworkSegmentTag
  type NetworkSegmentId = Int @@ NetworkSegmentTag
  
  object NetworkSegmentId {
    def apply(id: Int): NetworkSegmentId = tag[NetworkSegmentTag][Int](id)
  }
             
  /** Entity class storing rows of table NetworkSegmentTable
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param simulationOutcomeId Database column simulation_outcome_id SqlType(int4)
   *  @param taskId Database column task_id SqlType(int4)
   *  @param clientIp Database column client_ip SqlType(inet), Length(2147483647,false)
   *  @param subnetIp Database column subnet_ip SqlType(inet), Length(2147483647,false)
   *  @param subnetPrefix Database column subnet_prefix SqlType(int4)
   *  @param createdAt Database column created_at SqlType(timestamp) */
  case class NetworkSegment(id: NetworkSegmentId, simulationOutcomeId: SimulationOutcomeId, taskId: TaskId, clientIp: com.github.tminglei.slickpg.InetString, subnetIp: com.github.tminglei.slickpg.InetString, subnetPrefix: Int, createdAt: java.time.LocalDateTime)
  
  
  trait OutcomeTaskResultTag
  type OutcomeTaskResultId = Int @@ OutcomeTaskResultTag
  
  object OutcomeTaskResultId {
    def apply(id: Int): OutcomeTaskResultId = tag[OutcomeTaskResultTag][Int](id)
  }
             
  /** Entity class storing rows of table OutcomeTaskResultTable
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param simulationOutcomeId Database column simulation_outcome_id SqlType(int4)
   *  @param taskId Database column task_id SqlType(int4)
   *  @param isSuccess Database column is_success SqlType(bool)
   *  @param message Database column message SqlType(varchar), Default(None)
   *  @param taskResult Database column task_result SqlType(json), Length(2147483647,false), Default(None)
   *  @param duration Database column duration SqlType(interval), Length(49,false), Default(None)
   *  @param urlTestId Database column url_test_id SqlType(int4), Default(None) */
  case class OutcomeTaskResult(id: OutcomeTaskResultId, simulationOutcomeId: SimulationOutcomeId, taskId: TaskId, isSuccess: Boolean, message: Option[String] = None, taskResult: Option[play.api.libs.json.JsValue] = None, duration: Option[java.time.Duration] = None, urlTestId: Option[UrlTestId] = None)
  
  
  trait ReceivedDataTag
  type ReceivedDataId = Int @@ ReceivedDataTag
  
  object ReceivedDataId {
    def apply(id: Int): ReceivedDataId = tag[ReceivedDataTag][Int](id)
  }
             
  /** Entity class storing rows of table ReceivedDataTable
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param simulationOutcomeId Database column simulation_outcome_id SqlType(int4)
   *  @param taskId Database column task_id SqlType(int4)
   *  @param contentType Database column content_type SqlType(varchar), Length(30,true), Default(None)
   *  @param fileName Database column file_name SqlType(varchar), Length(30,true)
   *  @param createdAt Database column created_at SqlType(timestamp)
   *  @param size Database column size SqlType(int8), Default(0) */
  case class ReceivedData(id: ReceivedDataId, simulationOutcomeId: SimulationOutcomeId, taskId: TaskId, contentType: Option[String] = None, fileName: String, createdAt: java.time.LocalDateTime, size: Long = 0L)
  
  
  trait RunnerTag
  type RunnerId = Int @@ RunnerTag
  
  object RunnerId {
    def apply(id: Int): RunnerId = tag[RunnerTag][Int](id)
  }
             
  /** Entity class storing rows of table RunnerTable
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param runnerName Database column runner_name SqlType(varchar), Length(50,true) */
  case class Runner(id: RunnerId, runnerName: String)
  
  
  trait SimulationOutcomeTag
  type SimulationOutcomeId = Int @@ SimulationOutcomeTag
  
  object SimulationOutcomeId {
    def apply(id: Int): SimulationOutcomeId = tag[SimulationOutcomeTag][Int](id)
  }
             
  /** Entity class storing rows of table SimulationOutcomeTable
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param simulationId Database column simulation_id SqlType(int4)
   *  @param token Database column token SqlType(uuid)
   *  @param createdAt Database column created_at SqlType(timestamp)
   *  @param finishedAt Database column finished_at SqlType(timestamp), Default(None)
   *  @param lastPingAt Database column last_ping_at SqlType(timestamp) */
  case class SimulationOutcome(id: SimulationOutcomeId, simulationId: SimulationId, token: java.util.UUID, createdAt: java.time.LocalDateTime, finishedAt: Option[java.time.LocalDateTime] = None, lastPingAt: java.time.LocalDateTime)
  
  /** Entity class storing rows of table SimulationResultsTable
   *  @param id Database column id SqlType(int4)
   *  @param simulationToken Database column simulation_token SqlType(uuid), Default(None)
   *  @param simulationDescription Database column simulation_description SqlType(varchar), Length(50,true), Default(None)
   *  @param outcomeToken Database column outcome_token SqlType(uuid), Default(None)
   *  @param outcomeStarted Database column outcome_started SqlType(timestamp), Default(None)
   *  @param outcomeFinished Database column outcome_finished SqlType(timestamp), Default(None)
   *  @param taskKey Database column task_key SqlType(varchar), Length(32,true), Default(None)
   *  @param isSuccess Database column is_success SqlType(bool), Default(None)
   *  @param message Database column message SqlType(varchar), Default(None)
   *  @param taskResult Database column task_result SqlType(json), Length(2147483647,false), Default(None)
   *  @param duration Database column duration SqlType(interval), Length(49,false), Default(None)
   *  @param urlCheckAt Database column url_check_at SqlType(timestamp), Default(None)
   *  @param urlCheckResponseSize Database column url_check_response_size SqlType(int8), Default(None)
   *  @param urlCheckDuration Database column url_check_duration SqlType(interval), Length(49,false), Default(None) */
  case class SimulationResults(id: Int, simulationToken: Option[java.util.UUID] = None, simulationDescription: Option[String] = None, outcomeToken: Option[java.util.UUID] = None, outcomeStarted: Option[java.time.LocalDateTime] = None, outcomeFinished: Option[java.time.LocalDateTime] = None, taskKey: Option[String] = None, isSuccess: Option[Boolean] = None, message: Option[String] = None, taskResult: Option[play.api.libs.json.JsValue] = None, duration: Option[java.time.Duration] = None, urlCheckAt: Option[java.time.LocalDateTime] = None, urlCheckResponseSize: Option[Long] = None, urlCheckDuration: Option[java.time.Duration] = None)
  
  
  trait SimulationTag
  type SimulationId = Int @@ SimulationTag
  
  object SimulationId {
    def apply(id: Int): SimulationId = tag[SimulationTag][Int](id)
  }
             
  /** Entity class storing rows of table SimulationTable
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param userId Database column user_id SqlType(int4)
   *  @param token Database column token SqlType(uuid)
   *  @param description Database column description SqlType(varchar), Length(50,true)
   *  @param customerAssessmentId Database column customer_assessment_id SqlType(int4)
   *  @param templateId Database column template_id SqlType(int4) */
  case class Simulation(id: SimulationId, userId: UserId, token: java.util.UUID, description: Option[String], customerAssessmentId: public.Model.CustomerAssessmentId, templateId: SimulationTemplateId)
  
  /** Entity class storing rows of table SimulationTemplateConfigTable
   *  @param templateId Database column template_id SqlType(int4)
   *  @param taskId Database column task_id SqlType(int4)
   *  @param position Database column position SqlType(int4)
   *  @param testCaseId Database column test_case_id SqlType(int4), Default(None) */
  case class SimulationTemplateConfig(templateId: SimulationTemplateId, taskId: TaskId, position: Int, testCaseId: Option[TestCaseId] = None)
  
  
  trait SimulationTemplateTag
  type SimulationTemplateId = Int @@ SimulationTemplateTag
  
  object SimulationTemplateId {
    def apply(id: Int): SimulationTemplateId = tag[SimulationTemplateTag][Int](id)
  }
             
  /** Entity class storing rows of table SimulationTemplateTable
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param version Database column version SqlType(timestamp)
   *  @param assessmentTypeId Database column assessment_type_id SqlType(int4)
   *  @param description Database column description SqlType(varchar), Length(50,true), Default(None) */
  case class SimulationTemplate(id: SimulationTemplateId, version: java.time.LocalDateTime, assessmentTypeId: public.Model.AssessmentTypeId, description: Option[String] = None)
  
  /** Entity class storing rows of table TaskForSimulationTable
   *  @param id Database column id SqlType(int4)
   *  @param templateId Database column template_id SqlType(int4)
   *  @param position Database column position SqlType(int4)
   *  @param taskKey Database column task_key SqlType(varchar), Length(32,true)
   *  @param runnerName Database column runner_name SqlType(varchar), Length(50,true)
   *  @param labelInSimulation Database column label_in_simulation SqlType(varchar), Default(None)
   *  @param parameters Database column parameters SqlType(json), Length(2147483647,false), Default(None) */
  case class TaskForSimulation(id: TaskId, templateId: SimulationTemplateId, position: Int, taskKey: String, runnerName: String, labelInSimulation: Option[String] = None, parameters: Option[play.api.libs.json.JsValue] = None)
  
  
  trait TaskTag
  type TaskId = Int @@ TaskTag
  
  object TaskId {
    def apply(id: Int): TaskId = tag[TaskTag][Int](id)
  }
             
  /** Entity class storing rows of table TaskTable
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param runnerId Database column runner_id SqlType(int4)
   *  @param taskKey Database column task_key SqlType(varchar), Length(32,true)
   *  @param parameters Database column parameters SqlType(json), Length(2147483647,false), Default(None) */
  case class Task(id: TaskId, runnerId: RunnerId, taskKey: String, parameters: Option[play.api.libs.json.JsValue] = None)
  
  
  trait TestCaseTag
  type TestCaseId = Int @@ TestCaseTag
  
  object TestCaseId {
    def apply(id: Int): TestCaseId = tag[TestCaseTag][Int](id)
  }
             
  /** Entity class storing rows of table TestCaseTable
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param testCaseKey Database column test_case_key SqlType(varchar), Length(30,true)
   *  @param labelInSimulation Database column label_in_simulation SqlType(varchar)
   *  @param description Database column description SqlType(varchar) */
  case class TestCase(id: TestCaseId, testCaseKey: String, labelInSimulation: String, description: String)
  
  
  trait UrlTestTag
  type UrlTestId = Int @@ UrlTestTag
  
  object UrlTestId {
    def apply(id: Int): UrlTestId = tag[UrlTestTag][Int](id)
  }
             
  /** Entity class storing rows of table UrlTestTable
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param clientRequest Database column client_request SqlType(int4), Default(None)
   *  @param taskId Database column task_id SqlType(int4)
   *  @param size Database column size SqlType(int8), Default(0)
   *  @param duration Database column duration SqlType(interval), Length(49,false), Default(None)
   *  @param createdAt Database column created_at SqlType(timestamp)
   *  @param status Database column status SqlType(int4), Default(None) */
  case class UrlTest(id: UrlTestId, clientRequest: Option[ClientRequestId] = None, taskId: TaskId, size: Long = 0L, duration: Option[java.time.Duration] = None, createdAt: java.time.LocalDateTime, status: Option[Int] = None)
  
  
  trait UserTag
  type UserId = Int @@ UserTag
  
  object UserId {
    def apply(id: Int): UserId = tag[UserTag][Int](id)
  }
             
  /** Entity class storing rows of table UserTable
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param customerId Database column customer_id SqlType(int4)
   *  @param userName Database column user_name SqlType(varchar)
   *  @param campaignVisitorId Database column campaign_visitor_id SqlType(int4), Default(None) */
  case class User(id: UserId, customerId: public.Model.CustomerId, userName: String, campaignVisitorId: Option[public.Model.CampaignVisitorId] = None)
}
        
