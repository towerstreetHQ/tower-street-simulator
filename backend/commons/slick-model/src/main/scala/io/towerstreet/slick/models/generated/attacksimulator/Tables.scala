package io.towerstreet.slick.models.generated.attacksimulator
// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object Tables extends {
  val profile = io.towerstreet.slick.db.TsPostgresProfile
} with Tables

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait Tables {
  val profile: io.towerstreet.slick.db.TsPostgresProfile
  import profile.api._
  
  import io.towerstreet.slick.models.generated.attacksimulator.Model._
  import io.towerstreet.slick.models.generated._
  import io.towerstreet.slick.db.ColumnMappers._

  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = Array(CampaignVisitorSimulationTable.schema, ClientRequestTable.schema, NetworkSegmentTable.schema, OutcomeTaskResultTable.schema, ReceivedDataTable.schema, RunnerTable.schema, SimulationOutcomeTable.schema, SimulationResultsTable.schema, SimulationTable.schema, SimulationTemplateConfigTable.schema, SimulationTemplateTable.schema, TaskForSimulationTable.schema, TaskTable.schema, TestCaseTable.schema, UrlTestTable.schema, UserTable.schema).reduceLeft(_ ++ _)
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Table description of table campaign_visitor_simulation. Objects of this class serve as prototypes for rows in queries. */
  class CampaignVisitorSimulationTable(_tableTag: Tag) extends profile.api.Table[CampaignVisitorSimulation](_tableTag, Some("attacksimulator"), "campaign_visitor_simulation") {
    def * = (visitorId, customerId, simulationId, simulationToken, templateId, simulationOutcomeId, simulationOutcomeCreatedAt, simulationOutcomeFinishedAt, simulationOutcomeLastPingAt, scoringOutcomeId, scoringQueuedAt, scoringFinishedAt, simulationUserId) <> (CampaignVisitorSimulation.tupled, CampaignVisitorSimulation.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(visitorId), Rep.Some(customerId), Rep.Some(simulationId), Rep.Some(simulationToken), Rep.Some(templateId), simulationOutcomeId, simulationOutcomeCreatedAt, simulationOutcomeFinishedAt, simulationOutcomeLastPingAt, scoringOutcomeId, scoringQueuedAt, scoringFinishedAt, Rep.Some(simulationUserId)).shaped.<>({r=>import r._; _1.map(_=> CampaignVisitorSimulation.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6, _7, _8, _9, _10, _11, _12, _13.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column visitor_id SqlType(int4) */
    val visitorId: Rep[io.towerstreet.slick.models.generated.public.Model.CampaignVisitorId] = column[io.towerstreet.slick.models.generated.public.Model.CampaignVisitorId]("visitor_id")
    /** Database column customer_id SqlType(int4) */
    val customerId: Rep[io.towerstreet.slick.models.generated.public.Model.CustomerId] = column[io.towerstreet.slick.models.generated.public.Model.CustomerId]("customer_id")
    /** Database column simulation_id SqlType(int4) */
    val simulationId: Rep[io.towerstreet.slick.models.generated.attacksimulator.Model.SimulationId] = column[io.towerstreet.slick.models.generated.attacksimulator.Model.SimulationId]("simulation_id")
    /** Database column simulation_token SqlType(uuid) */
    val simulationToken: Rep[java.util.UUID] = column[java.util.UUID]("simulation_token")
    /** Database column template_id SqlType(int4) */
    val templateId: Rep[io.towerstreet.slick.models.generated.attacksimulator.Model.SimulationTemplateId] = column[io.towerstreet.slick.models.generated.attacksimulator.Model.SimulationTemplateId]("template_id")
    /** Database column simulation_outcome_id SqlType(int4), Default(None) */
    val simulationOutcomeId: Rep[Option[io.towerstreet.slick.models.generated.attacksimulator.Model.SimulationOutcomeId]] = column[Option[io.towerstreet.slick.models.generated.attacksimulator.Model.SimulationOutcomeId]]("simulation_outcome_id", O.Default(None))
    /** Database column simulation_outcome_created_at SqlType(timestamp), Default(None) */
    val simulationOutcomeCreatedAt: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("simulation_outcome_created_at", O.Default(None))
    /** Database column simulation_outcome_finished_at SqlType(timestamp), Default(None) */
    val simulationOutcomeFinishedAt: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("simulation_outcome_finished_at", O.Default(None))
    /** Database column simulation_outcome_last_ping_at SqlType(timestamp), Default(None) */
    val simulationOutcomeLastPingAt: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("simulation_outcome_last_ping_at", O.Default(None))
    /** Database column scoring_outcome_id SqlType(int4), Default(None) */
    val scoringOutcomeId: Rep[Option[io.towerstreet.slick.models.generated.scoring.Model.ScoringOutcomeId]] = column[Option[io.towerstreet.slick.models.generated.scoring.Model.ScoringOutcomeId]]("scoring_outcome_id", O.Default(None))
    /** Database column scoring_queued_at SqlType(timestamp), Default(None) */
    val scoringQueuedAt: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("scoring_queued_at", O.Default(None))
    /** Database column scoring_finished_at SqlType(timestamp), Default(None) */
    val scoringFinishedAt: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("scoring_finished_at", O.Default(None))
    /** Database column simulation_user_id SqlType(int4) */
    val simulationUserId: Rep[io.towerstreet.slick.models.generated.attacksimulator.Model.UserId] = column[io.towerstreet.slick.models.generated.attacksimulator.Model.UserId]("simulation_user_id")
  }
  /** Collection-like TableQuery object for table CampaignVisitorSimulationTable */
  lazy val CampaignVisitorSimulationTable = new TableQuery(tag => new CampaignVisitorSimulationTable(tag))

  /** Table description of table client_request. Objects of this class serve as prototypes for rows in queries. */
  class ClientRequestTable(_tableTag: Tag) extends profile.api.Table[ClientRequest](_tableTag, Some("attacksimulator"), "client_request") {
    def * = (id, inetAddr, userAgent, requestedAt, resource, responseStatus, simulationId, simulationOutcomeId, receivedDataId, token) <> (ClientRequest.tupled, ClientRequest.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(inetAddr), userAgent, Rep.Some(requestedAt), Rep.Some(resource), Rep.Some(responseStatus), simulationId, simulationOutcomeId, receivedDataId, token).shaped.<>({r=>import r._; _1.map(_=> ClientRequest.tupled((_1.get, _2.get, _3, _4.get, _5.get, _6.get, _7, _8, _9, _10)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[ClientRequestId] = column[ClientRequestId]("id", O.AutoInc, O.PrimaryKey)
    /** Database column inet_addr SqlType(inet), Length(2147483647,false) */
    val inetAddr: Rep[com.github.tminglei.slickpg.InetString] = column[com.github.tminglei.slickpg.InetString]("inet_addr", O.Length(2147483647,varying=false))
    /** Database column user_agent SqlType(varchar), Default(None) */
    val userAgent: Rep[Option[String]] = column[Option[String]]("user_agent", O.Default(None))
    /** Database column requested_at SqlType(timestamp) */
    val requestedAt: Rep[java.time.LocalDateTime] = column[java.time.LocalDateTime]("requested_at")
    /** Database column resource SqlType(varchar) */
    val resource: Rep[String] = column[String]("resource")
    /** Database column response_status SqlType(varchar) */
    val responseStatus: Rep[io.towerstreet.slick.models.attacksimulator.enums.ClientRequestStatus] = column[io.towerstreet.slick.models.attacksimulator.enums.ClientRequestStatus]("response_status")
    /** Database column simulation_id SqlType(int4), Default(None) */
    val simulationId: Rep[Option[SimulationId]] = column[Option[SimulationId]]("simulation_id", O.Default(None))
    /** Database column simulation_outcome_id SqlType(int4), Default(None) */
    val simulationOutcomeId: Rep[Option[SimulationOutcomeId]] = column[Option[SimulationOutcomeId]]("simulation_outcome_id", O.Default(None))
    /** Database column received_data_id SqlType(int4), Default(None) */
    val receivedDataId: Rep[Option[ReceivedDataId]] = column[Option[ReceivedDataId]]("received_data_id", O.Default(None))
    /** Database column token SqlType(uuid), Default(None) */
    val token: Rep[Option[java.util.UUID]] = column[Option[java.util.UUID]]("token", O.Default(None))
  }
  /** Collection-like TableQuery object for table ClientRequestTable */
  lazy val ClientRequestTable = new TableQuery(tag => new ClientRequestTable(tag))

  /** Collection-like TableQuery object resolving inserted object's primary key for table OutcomeTaskResultTable */
  lazy val ClientRequestTableInsert = ClientRequestTable returning ClientRequestTable.map(_.id) into ((item, id) => item.copy(id = id))
             

  /** Table description of table network_segment. Objects of this class serve as prototypes for rows in queries. */
  class NetworkSegmentTable(_tableTag: Tag) extends profile.api.Table[NetworkSegment](_tableTag, Some("attacksimulator"), "network_segment") {
    def * = (id, simulationOutcomeId, taskId, clientIp, subnetIp, subnetPrefix, createdAt) <> (NetworkSegment.tupled, NetworkSegment.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(simulationOutcomeId), Rep.Some(taskId), Rep.Some(clientIp), Rep.Some(subnetIp), Rep.Some(subnetPrefix), Rep.Some(createdAt)).shaped.<>({r=>import r._; _1.map(_=> NetworkSegment.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[NetworkSegmentId] = column[NetworkSegmentId]("id", O.AutoInc, O.PrimaryKey)
    /** Database column simulation_outcome_id SqlType(int4) */
    val simulationOutcomeId: Rep[SimulationOutcomeId] = column[SimulationOutcomeId]("simulation_outcome_id")
    /** Database column task_id SqlType(int4) */
    val taskId: Rep[TaskId] = column[TaskId]("task_id")
    /** Database column client_ip SqlType(inet), Length(2147483647,false) */
    val clientIp: Rep[com.github.tminglei.slickpg.InetString] = column[com.github.tminglei.slickpg.InetString]("client_ip", O.Length(2147483647,varying=false))
    /** Database column subnet_ip SqlType(inet), Length(2147483647,false) */
    val subnetIp: Rep[com.github.tminglei.slickpg.InetString] = column[com.github.tminglei.slickpg.InetString]("subnet_ip", O.Length(2147483647,varying=false))
    /** Database column subnet_prefix SqlType(int4) */
    val subnetPrefix: Rep[Int] = column[Int]("subnet_prefix")
    /** Database column created_at SqlType(timestamp) */
    val createdAt: Rep[java.time.LocalDateTime] = column[java.time.LocalDateTime]("created_at")
  }
  /** Collection-like TableQuery object for table NetworkSegmentTable */
  lazy val NetworkSegmentTable = new TableQuery(tag => new NetworkSegmentTable(tag))

  /** Collection-like TableQuery object resolving inserted object's primary key for table OutcomeTaskResultTable */
  lazy val NetworkSegmentTableInsert = NetworkSegmentTable returning NetworkSegmentTable.map(_.id) into ((item, id) => item.copy(id = id))
             

  /** Table description of table outcome_task_result. Objects of this class serve as prototypes for rows in queries. */
  class OutcomeTaskResultTable(_tableTag: Tag) extends profile.api.Table[OutcomeTaskResult](_tableTag, Some("attacksimulator"), "outcome_task_result") {
    def * = (id, simulationOutcomeId, taskId, isSuccess, message, taskResult, duration, urlTestId) <> (OutcomeTaskResult.tupled, OutcomeTaskResult.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(simulationOutcomeId), Rep.Some(taskId), Rep.Some(isSuccess), message, taskResult, duration, urlTestId).shaped.<>({r=>import r._; _1.map(_=> OutcomeTaskResult.tupled((_1.get, _2.get, _3.get, _4.get, _5, _6, _7, _8)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[OutcomeTaskResultId] = column[OutcomeTaskResultId]("id", O.AutoInc, O.PrimaryKey)
    /** Database column simulation_outcome_id SqlType(int4) */
    val simulationOutcomeId: Rep[SimulationOutcomeId] = column[SimulationOutcomeId]("simulation_outcome_id")
    /** Database column task_id SqlType(int4) */
    val taskId: Rep[TaskId] = column[TaskId]("task_id")
    /** Database column is_success SqlType(bool) */
    val isSuccess: Rep[Boolean] = column[Boolean]("is_success")
    /** Database column message SqlType(varchar), Default(None) */
    val message: Rep[Option[String]] = column[Option[String]]("message", O.Default(None))
    /** Database column task_result SqlType(json), Length(2147483647,false), Default(None) */
    val taskResult: Rep[Option[play.api.libs.json.JsValue]] = column[Option[play.api.libs.json.JsValue]]("task_result", O.Length(2147483647,varying=false), O.Default(None))
    /** Database column duration SqlType(interval), Length(49,false), Default(None) */
    val duration: Rep[Option[java.time.Duration]] = column[Option[java.time.Duration]]("duration", O.Length(49,varying=false), O.Default(None))
    /** Database column url_test_id SqlType(int4), Default(None) */
    val urlTestId: Rep[Option[UrlTestId]] = column[Option[UrlTestId]]("url_test_id", O.Default(None))
  }
  /** Collection-like TableQuery object for table OutcomeTaskResultTable */
  lazy val OutcomeTaskResultTable = new TableQuery(tag => new OutcomeTaskResultTable(tag))

  /** Collection-like TableQuery object resolving inserted object's primary key for table OutcomeTaskResultTable */
  lazy val OutcomeTaskResultTableInsert = OutcomeTaskResultTable returning OutcomeTaskResultTable.map(_.id) into ((item, id) => item.copy(id = id))
             

  /** Table description of table received_data. Objects of this class serve as prototypes for rows in queries. */
  class ReceivedDataTable(_tableTag: Tag) extends profile.api.Table[ReceivedData](_tableTag, Some("attacksimulator"), "received_data") {
    def * = (id, simulationOutcomeId, taskId, contentType, fileName, createdAt, size) <> (ReceivedData.tupled, ReceivedData.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(simulationOutcomeId), Rep.Some(taskId), contentType, Rep.Some(fileName), Rep.Some(createdAt), Rep.Some(size)).shaped.<>({r=>import r._; _1.map(_=> ReceivedData.tupled((_1.get, _2.get, _3.get, _4, _5.get, _6.get, _7.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[ReceivedDataId] = column[ReceivedDataId]("id", O.AutoInc, O.PrimaryKey)
    /** Database column simulation_outcome_id SqlType(int4) */
    val simulationOutcomeId: Rep[SimulationOutcomeId] = column[SimulationOutcomeId]("simulation_outcome_id")
    /** Database column task_id SqlType(int4) */
    val taskId: Rep[TaskId] = column[TaskId]("task_id")
    /** Database column content_type SqlType(varchar), Length(30,true), Default(None) */
    val contentType: Rep[Option[String]] = column[Option[String]]("content_type", O.Length(30,varying=true), O.Default(None))
    /** Database column file_name SqlType(varchar), Length(30,true) */
    val fileName: Rep[String] = column[String]("file_name", O.Length(30,varying=true))
    /** Database column created_at SqlType(timestamp) */
    val createdAt: Rep[java.time.LocalDateTime] = column[java.time.LocalDateTime]("created_at")
    /** Database column size SqlType(int8), Default(0) */
    val size: Rep[Long] = column[Long]("size", O.Default(0L))
  }
  /** Collection-like TableQuery object for table ReceivedDataTable */
  lazy val ReceivedDataTable = new TableQuery(tag => new ReceivedDataTable(tag))

  /** Collection-like TableQuery object resolving inserted object's primary key for table OutcomeTaskResultTable */
  lazy val ReceivedDataTableInsert = ReceivedDataTable returning ReceivedDataTable.map(_.id) into ((item, id) => item.copy(id = id))
             

  /** Table description of table runner. Objects of this class serve as prototypes for rows in queries. */
  class RunnerTable(_tableTag: Tag) extends profile.api.Table[Runner](_tableTag, Some("attacksimulator"), "runner") {
    def * = (id, runnerName) <> (Runner.tupled, Runner.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(runnerName)).shaped.<>({r=>import r._; _1.map(_=> Runner.tupled((_1.get, _2.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[RunnerId] = column[RunnerId]("id", O.AutoInc, O.PrimaryKey)
    /** Database column runner_name SqlType(varchar), Length(50,true) */
    val runnerName: Rep[String] = column[String]("runner_name", O.Length(50,varying=true))

    /** Uniqueness Index over (runnerName) (database name runner_runner_name_key) */
    val index1 = index("runner_runner_name_key", runnerName, unique=true)
  }
  /** Collection-like TableQuery object for table RunnerTable */
  lazy val RunnerTable = new TableQuery(tag => new RunnerTable(tag))

  /** Collection-like TableQuery object resolving inserted object's primary key for table OutcomeTaskResultTable */
  lazy val RunnerTableInsert = RunnerTable returning RunnerTable.map(_.id) into ((item, id) => item.copy(id = id))
             

  /** Table description of table simulation_outcome. Objects of this class serve as prototypes for rows in queries. */
  class SimulationOutcomeTable(_tableTag: Tag) extends profile.api.Table[SimulationOutcome](_tableTag, Some("attacksimulator"), "simulation_outcome") {
    def * = (id, simulationId, token, createdAt, finishedAt, lastPingAt) <> (SimulationOutcome.tupled, SimulationOutcome.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(simulationId), Rep.Some(token), Rep.Some(createdAt), finishedAt, Rep.Some(lastPingAt)).shaped.<>({r=>import r._; _1.map(_=> SimulationOutcome.tupled((_1.get, _2.get, _3.get, _4.get, _5, _6.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[SimulationOutcomeId] = column[SimulationOutcomeId]("id", O.AutoInc, O.PrimaryKey)
    /** Database column simulation_id SqlType(int4) */
    val simulationId: Rep[SimulationId] = column[SimulationId]("simulation_id")
    /** Database column token SqlType(uuid) */
    val token: Rep[java.util.UUID] = column[java.util.UUID]("token")
    /** Database column created_at SqlType(timestamp) */
    val createdAt: Rep[java.time.LocalDateTime] = column[java.time.LocalDateTime]("created_at")
    /** Database column finished_at SqlType(timestamp), Default(None) */
    val finishedAt: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("finished_at", O.Default(None))
    /** Database column last_ping_at SqlType(timestamp) */
    val lastPingAt: Rep[java.time.LocalDateTime] = column[java.time.LocalDateTime]("last_ping_at")

    /** Uniqueness Index over (token) (database name simulation_outcome_token_key) */
    val index1 = index("simulation_outcome_token_key", token, unique=true)
  }
  /** Collection-like TableQuery object for table SimulationOutcomeTable */
  lazy val SimulationOutcomeTable = new TableQuery(tag => new SimulationOutcomeTable(tag))

  /** Collection-like TableQuery object resolving inserted object's primary key for table OutcomeTaskResultTable */
  lazy val SimulationOutcomeTableInsert = SimulationOutcomeTable returning SimulationOutcomeTable.map(_.id) into ((item, id) => item.copy(id = id))
             

  /** Table description of table simulation_results. Objects of this class serve as prototypes for rows in queries. */
  class SimulationResultsTable(_tableTag: Tag) extends profile.api.Table[SimulationResults](_tableTag, Some("attacksimulator"), "simulation_results") {
    def * = (id, simulationToken, simulationDescription, outcomeToken, outcomeStarted, outcomeFinished, taskKey, isSuccess, message, taskResult, duration, urlCheckAt, urlCheckResponseSize, urlCheckDuration) <> (SimulationResults.tupled, SimulationResults.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), simulationToken, simulationDescription, outcomeToken, outcomeStarted, outcomeFinished, taskKey, isSuccess, message, taskResult, duration, urlCheckAt, urlCheckResponseSize, urlCheckDuration).shaped.<>({r=>import r._; _1.map(_=> SimulationResults.tupled((_1.get, _2, _3, _4, _5, _6, _7, _8, _9, _10, _11, _12, _13, _14)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(int4) */
    val id: Rep[Int] = column[Int]("id")
    /** Database column simulation_token SqlType(uuid), Default(None) */
    val simulationToken: Rep[Option[java.util.UUID]] = column[Option[java.util.UUID]]("simulation_token", O.Default(None))
    /** Database column simulation_description SqlType(varchar), Length(50,true), Default(None) */
    val simulationDescription: Rep[Option[String]] = column[Option[String]]("simulation_description", O.Length(50,varying=true), O.Default(None))
    /** Database column outcome_token SqlType(uuid), Default(None) */
    val outcomeToken: Rep[Option[java.util.UUID]] = column[Option[java.util.UUID]]("outcome_token", O.Default(None))
    /** Database column outcome_started SqlType(timestamp), Default(None) */
    val outcomeStarted: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("outcome_started", O.Default(None))
    /** Database column outcome_finished SqlType(timestamp), Default(None) */
    val outcomeFinished: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("outcome_finished", O.Default(None))
    /** Database column task_key SqlType(varchar), Length(32,true), Default(None) */
    val taskKey: Rep[Option[String]] = column[Option[String]]("task_key", O.Length(32,varying=true), O.Default(None))
    /** Database column is_success SqlType(bool), Default(None) */
    val isSuccess: Rep[Option[Boolean]] = column[Option[Boolean]]("is_success", O.Default(None))
    /** Database column message SqlType(varchar), Default(None) */
    val message: Rep[Option[String]] = column[Option[String]]("message", O.Default(None))
    /** Database column task_result SqlType(json), Length(2147483647,false), Default(None) */
    val taskResult: Rep[Option[play.api.libs.json.JsValue]] = column[Option[play.api.libs.json.JsValue]]("task_result", O.Length(2147483647,varying=false), O.Default(None))
    /** Database column duration SqlType(interval), Length(49,false), Default(None) */
    val duration: Rep[Option[java.time.Duration]] = column[Option[java.time.Duration]]("duration", O.Length(49,varying=false), O.Default(None))
    /** Database column url_check_at SqlType(timestamp), Default(None) */
    val urlCheckAt: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("url_check_at", O.Default(None))
    /** Database column url_check_response_size SqlType(int8), Default(None) */
    val urlCheckResponseSize: Rep[Option[Long]] = column[Option[Long]]("url_check_response_size", O.Default(None))
    /** Database column url_check_duration SqlType(interval), Length(49,false), Default(None) */
    val urlCheckDuration: Rep[Option[java.time.Duration]] = column[Option[java.time.Duration]]("url_check_duration", O.Length(49,varying=false), O.Default(None))
  }
  /** Collection-like TableQuery object for table SimulationResultsTable */
  lazy val SimulationResultsTable = new TableQuery(tag => new SimulationResultsTable(tag))

  /** Table description of table simulation. Objects of this class serve as prototypes for rows in queries. */
  class SimulationTable(_tableTag: Tag) extends profile.api.Table[Simulation](_tableTag, Some("attacksimulator"), "simulation") {
    def * = (id, userId, token, description, customerAssessmentId, templateId) <> (Simulation.tupled, Simulation.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(userId), Rep.Some(token), description, Rep.Some(customerAssessmentId), Rep.Some(templateId)).shaped.<>({r=>import r._; _1.map(_=> Simulation.tupled((_1.get, _2.get, _3.get, _4, _5.get, _6.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[SimulationId] = column[SimulationId]("id", O.AutoInc, O.PrimaryKey)
    /** Database column user_id SqlType(int4) */
    val userId: Rep[UserId] = column[UserId]("user_id")
    /** Database column token SqlType(uuid) */
    val token: Rep[java.util.UUID] = column[java.util.UUID]("token")
    /** Database column description SqlType(varchar), Length(50,true) */
    val description: Rep[Option[String]] = column[Option[String]]("description", O.Length(50,varying=true))
    /** Database column customer_assessment_id SqlType(int4) */
    val customerAssessmentId: Rep[public.Model.CustomerAssessmentId] = column[public.Model.CustomerAssessmentId]("customer_assessment_id")
    /** Database column template_id SqlType(int4) */
    val templateId: Rep[SimulationTemplateId] = column[SimulationTemplateId]("template_id")

    /** Uniqueness Index over (token) (database name simulation_token_key) */
    val index1 = index("simulation_token_key", token, unique=true)
  }
  /** Collection-like TableQuery object for table SimulationTable */
  lazy val SimulationTable = new TableQuery(tag => new SimulationTable(tag))

  /** Collection-like TableQuery object resolving inserted object's primary key for table OutcomeTaskResultTable */
  lazy val SimulationTableInsert = SimulationTable returning SimulationTable.map(_.id) into ((item, id) => item.copy(id = id))
             

  /** Table description of table simulation_template_config. Objects of this class serve as prototypes for rows in queries. */
  class SimulationTemplateConfigTable(_tableTag: Tag) extends profile.api.Table[SimulationTemplateConfig](_tableTag, Some("attacksimulator"), "simulation_template_config") {
    def * = (templateId, taskId, position, testCaseId) <> (SimulationTemplateConfig.tupled, SimulationTemplateConfig.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(templateId), Rep.Some(taskId), Rep.Some(position), testCaseId).shaped.<>({r=>import r._; _1.map(_=> SimulationTemplateConfig.tupled((_1.get, _2.get, _3.get, _4)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column template_id SqlType(int4) */
    val templateId: Rep[SimulationTemplateId] = column[SimulationTemplateId]("template_id")
    /** Database column task_id SqlType(int4) */
    val taskId: Rep[TaskId] = column[TaskId]("task_id")
    /** Database column position SqlType(int4) */
    val position: Rep[Int] = column[Int]("position")
    /** Database column test_case_id SqlType(int4), Default(None) */
    val testCaseId: Rep[Option[TestCaseId]] = column[Option[TestCaseId]]("test_case_id", O.Default(None))
  }
  /** Collection-like TableQuery object for table SimulationTemplateConfigTable */
  lazy val SimulationTemplateConfigTable = new TableQuery(tag => new SimulationTemplateConfigTable(tag))

  /** Table description of table simulation_template. Objects of this class serve as prototypes for rows in queries. */
  class SimulationTemplateTable(_tableTag: Tag) extends profile.api.Table[SimulationTemplate](_tableTag, Some("attacksimulator"), "simulation_template") {
    def * = (id, version, assessmentTypeId, description) <> (SimulationTemplate.tupled, SimulationTemplate.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(version), Rep.Some(assessmentTypeId), description).shaped.<>({r=>import r._; _1.map(_=> SimulationTemplate.tupled((_1.get, _2.get, _3.get, _4)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[SimulationTemplateId] = column[SimulationTemplateId]("id", O.AutoInc, O.PrimaryKey)
    /** Database column version SqlType(timestamp) */
    val version: Rep[java.time.LocalDateTime] = column[java.time.LocalDateTime]("version")
    /** Database column assessment_type_id SqlType(int4) */
    val assessmentTypeId: Rep[public.Model.AssessmentTypeId] = column[public.Model.AssessmentTypeId]("assessment_type_id")
    /** Database column description SqlType(varchar), Length(50,true), Default(None) */
    val description: Rep[Option[String]] = column[Option[String]]("description", O.Length(50,varying=true), O.Default(None))

    /** Uniqueness Index over (version,assessmentTypeId) (database name simulation_template_version_assessment_type_id_key) */
    val index1 = index("simulation_template_version_assessment_type_id_key", (version, assessmentTypeId), unique=true)
  }
  /** Collection-like TableQuery object for table SimulationTemplateTable */
  lazy val SimulationTemplateTable = new TableQuery(tag => new SimulationTemplateTable(tag))

  /** Collection-like TableQuery object resolving inserted object's primary key for table OutcomeTaskResultTable */
  lazy val SimulationTemplateTableInsert = SimulationTemplateTable returning SimulationTemplateTable.map(_.id) into ((item, id) => item.copy(id = id))
             

  /** Table description of table task_for_simulation. Objects of this class serve as prototypes for rows in queries. */
  class TaskForSimulationTable(_tableTag: Tag) extends profile.api.Table[TaskForSimulation](_tableTag, Some("attacksimulator"), "task_for_simulation") {
    def * = (id, templateId, position, taskKey, runnerName, labelInSimulation, parameters) <> (TaskForSimulation.tupled, TaskForSimulation.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(templateId), Rep.Some(position), Rep.Some(taskKey), Rep.Some(runnerName), labelInSimulation, parameters).shaped.<>({r=>import r._; _1.map(_=> TaskForSimulation.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6, _7)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(int4) */
    val id: Rep[TaskId] = column[TaskId]("id")
    /** Database column template_id SqlType(int4) */
    val templateId: Rep[SimulationTemplateId] = column[SimulationTemplateId]("template_id")
    /** Database column position SqlType(int4) */
    val position: Rep[Int] = column[Int]("position")
    /** Database column task_key SqlType(varchar), Length(32,true) */
    val taskKey: Rep[String] = column[String]("task_key", O.Length(32,varying=true))
    /** Database column runner_name SqlType(varchar), Length(50,true) */
    val runnerName: Rep[String] = column[String]("runner_name", O.Length(50,varying=true))
    /** Database column label_in_simulation SqlType(varchar), Default(None) */
    val labelInSimulation: Rep[Option[String]] = column[Option[String]]("label_in_simulation", O.Default(None))
    /** Database column parameters SqlType(json), Length(2147483647,false), Default(None) */
    val parameters: Rep[Option[play.api.libs.json.JsValue]] = column[Option[play.api.libs.json.JsValue]]("parameters", O.Length(2147483647,varying=false), O.Default(None))
  }
  /** Collection-like TableQuery object for table TaskForSimulationTable */
  lazy val TaskForSimulationTable = new TableQuery(tag => new TaskForSimulationTable(tag))

  /** Table description of table task. Objects of this class serve as prototypes for rows in queries. */
  class TaskTable(_tableTag: Tag) extends profile.api.Table[Task](_tableTag, Some("attacksimulator"), "task") {
    def * = (id, runnerId, taskKey, parameters) <> (Task.tupled, Task.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(runnerId), Rep.Some(taskKey), parameters).shaped.<>({r=>import r._; _1.map(_=> Task.tupled((_1.get, _2.get, _3.get, _4)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[TaskId] = column[TaskId]("id", O.AutoInc, O.PrimaryKey)
    /** Database column runner_id SqlType(int4) */
    val runnerId: Rep[RunnerId] = column[RunnerId]("runner_id")
    /** Database column task_key SqlType(varchar), Length(32,true) */
    val taskKey: Rep[String] = column[String]("task_key", O.Length(32,varying=true))
    /** Database column parameters SqlType(json), Length(2147483647,false), Default(None) */
    val parameters: Rep[Option[play.api.libs.json.JsValue]] = column[Option[play.api.libs.json.JsValue]]("parameters", O.Length(2147483647,varying=false), O.Default(None))

    /** Uniqueness Index over (taskKey) (database name task_task_key_key) */
    val index1 = index("task_task_key_key", taskKey, unique=true)
  }
  /** Collection-like TableQuery object for table TaskTable */
  lazy val TaskTable = new TableQuery(tag => new TaskTable(tag))

  /** Collection-like TableQuery object resolving inserted object's primary key for table OutcomeTaskResultTable */
  lazy val TaskTableInsert = TaskTable returning TaskTable.map(_.id) into ((item, id) => item.copy(id = id))
             

  /** Table description of table test_case. Objects of this class serve as prototypes for rows in queries. */
  class TestCaseTable(_tableTag: Tag) extends profile.api.Table[TestCase](_tableTag, Some("attacksimulator"), "test_case") {
    def * = (id, testCaseKey, labelInSimulation, description) <> (TestCase.tupled, TestCase.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(testCaseKey), Rep.Some(labelInSimulation), Rep.Some(description)).shaped.<>({r=>import r._; _1.map(_=> TestCase.tupled((_1.get, _2.get, _3.get, _4.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[TestCaseId] = column[TestCaseId]("id", O.AutoInc, O.PrimaryKey)
    /** Database column test_case_key SqlType(varchar), Length(30,true) */
    val testCaseKey: Rep[String] = column[String]("test_case_key", O.Length(30,varying=true))
    /** Database column label_in_simulation SqlType(varchar) */
    val labelInSimulation: Rep[String] = column[String]("label_in_simulation")
    /** Database column description SqlType(varchar) */
    val description: Rep[String] = column[String]("description")

    /** Uniqueness Index over (testCaseKey) (database name test_case_test_case_key_key) */
    val index1 = index("test_case_test_case_key_key", testCaseKey, unique=true)
  }
  /** Collection-like TableQuery object for table TestCaseTable */
  lazy val TestCaseTable = new TableQuery(tag => new TestCaseTable(tag))

  /** Collection-like TableQuery object resolving inserted object's primary key for table OutcomeTaskResultTable */
  lazy val TestCaseTableInsert = TestCaseTable returning TestCaseTable.map(_.id) into ((item, id) => item.copy(id = id))
             

  /** Table description of table url_test. Objects of this class serve as prototypes for rows in queries. */
  class UrlTestTable(_tableTag: Tag) extends profile.api.Table[UrlTest](_tableTag, Some("attacksimulator"), "url_test") {
    def * = (id, clientRequest, taskId, size, duration, createdAt, status) <> (UrlTest.tupled, UrlTest.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), clientRequest, Rep.Some(taskId), Rep.Some(size), duration, Rep.Some(createdAt), status).shaped.<>({r=>import r._; _1.map(_=> UrlTest.tupled((_1.get, _2, _3.get, _4.get, _5, _6.get, _7)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[UrlTestId] = column[UrlTestId]("id", O.AutoInc, O.PrimaryKey)
    /** Database column client_request SqlType(int4), Default(None) */
    val clientRequest: Rep[Option[ClientRequestId]] = column[Option[ClientRequestId]]("client_request", O.Default(None))
    /** Database column task_id SqlType(int4) */
    val taskId: Rep[TaskId] = column[TaskId]("task_id")
    /** Database column size SqlType(int8), Default(0) */
    val size: Rep[Long] = column[Long]("size", O.Default(0L))
    /** Database column duration SqlType(interval), Length(49,false), Default(None) */
    val duration: Rep[Option[java.time.Duration]] = column[Option[java.time.Duration]]("duration", O.Length(49,varying=false), O.Default(None))
    /** Database column created_at SqlType(timestamp) */
    val createdAt: Rep[java.time.LocalDateTime] = column[java.time.LocalDateTime]("created_at")
    /** Database column status SqlType(int4), Default(None) */
    val status: Rep[Option[Int]] = column[Option[Int]]("status", O.Default(None))

    /** Index over (createdAt) (database name as_url_test_created_at_idx) */
    val index1 = index("as_url_test_created_at_idx", createdAt)
    /** Index over (taskId,createdAt) (database name url_test_created_at_idx) */
    val index2 = index("url_test_created_at_idx", (taskId, createdAt))
  }
  /** Collection-like TableQuery object for table UrlTestTable */
  lazy val UrlTestTable = new TableQuery(tag => new UrlTestTable(tag))

  /** Collection-like TableQuery object resolving inserted object's primary key for table OutcomeTaskResultTable */
  lazy val UrlTestTableInsert = UrlTestTable returning UrlTestTable.map(_.id) into ((item, id) => item.copy(id = id))
             

  /** Table description of table user. Objects of this class serve as prototypes for rows in queries. */
  class UserTable(_tableTag: Tag) extends profile.api.Table[User](_tableTag, Some("attacksimulator"), "user") {
    def * = (id, customerId, userName, campaignVisitorId) <> (User.tupled, User.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(customerId), Rep.Some(userName), campaignVisitorId).shaped.<>({r=>import r._; _1.map(_=> User.tupled((_1.get, _2.get, _3.get, _4)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[UserId] = column[UserId]("id", O.AutoInc, O.PrimaryKey)
    /** Database column customer_id SqlType(int4) */
    val customerId: Rep[public.Model.CustomerId] = column[public.Model.CustomerId]("customer_id")
    /** Database column user_name SqlType(varchar) */
    val userName: Rep[String] = column[String]("user_name")
    /** Database column campaign_visitor_id SqlType(int4), Default(None) */
    val campaignVisitorId: Rep[Option[public.Model.CampaignVisitorId]] = column[Option[public.Model.CampaignVisitorId]]("campaign_visitor_id", O.Default(None))

    /** Uniqueness Index over (customerId,userName) (database name user_company_id_user_name_key) */
    val index1 = index("user_company_id_user_name_key", (customerId, userName), unique=true)
  }
  /** Collection-like TableQuery object for table UserTable */
  lazy val UserTable = new TableQuery(tag => new UserTable(tag))

  /** Collection-like TableQuery object resolving inserted object's primary key for table OutcomeTaskResultTable */
  lazy val UserTableInsert = UserTable returning UserTable.map(_.id) into ((item, id) => item.copy(id = id))
             
          
}
