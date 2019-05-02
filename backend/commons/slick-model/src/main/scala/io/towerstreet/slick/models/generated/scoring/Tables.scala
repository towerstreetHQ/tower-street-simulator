package io.towerstreet.slick.models.generated.scoring
// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object Tables extends {
  val profile = io.towerstreet.slick.db.TsPostgresProfile
} with Tables

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait Tables {
  val profile: io.towerstreet.slick.db.TsPostgresProfile
  import profile.api._
  
  import io.towerstreet.slick.models.generated.scoring.Model._
  import io.towerstreet.slick.models.generated._
  import io.towerstreet.slick.db.ColumnMappers._

  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = Array(CampaignScoreHistogramTable.schema, ScoringDefinitionTable.schema, ScoringDefinitionWithTemplateTable.schema, ScoringOutcomeTable.schema, ScoringResultTable.schema, SimulationOutcomeForScoringTable.schema, SimulationScoringConfigTable.schema, SimulationScoringResultTable.schema).reduceLeft(_ ++ _)
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Table description of table campaign_score_histogram. Objects of this class serve as prototypes for rows in queries. */
  class CampaignScoreHistogramTable(_tableTag: Tag) extends profile.api.Table[CampaignScoreHistogram](_tableTag, Some("scoring"), "campaign_score_histogram") {
    def * = (customerId, score, value) <> (CampaignScoreHistogram.tupled, CampaignScoreHistogram.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(customerId), Rep.Some(score), Rep.Some(value)).shaped.<>({r=>import r._; _1.map(_=> CampaignScoreHistogram.tupled((_1.get, _2.get, _3.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column customer_id SqlType(int4) */
    val customerId: Rep[public.Model.CustomerId] = column[public.Model.CustomerId]("customer_id")
    /** Database column score SqlType(int4) */
    val score: Rep[Int] = column[Int]("score")
    /** Database column value SqlType(int4), Default(1) */
    val value: Rep[Int] = column[Int]("value", O.Default(1))

    /** Primary key of CampaignScoreHistogramTable (database name campaign_score_histogram_pkey) */
    val pk = primaryKey("campaign_score_histogram_pkey", (customerId, score))
  }
  /** Collection-like TableQuery object for table CampaignScoreHistogramTable */
  lazy val CampaignScoreHistogramTable = new TableQuery(tag => new CampaignScoreHistogramTable(tag))

  /** Table description of table scoring_definition. Objects of this class serve as prototypes for rows in queries. */
  class ScoringDefinitionTable(_tableTag: Tag) extends profile.api.Table[ScoringDefinition](_tableTag, Some("scoring"), "scoring_definition") {
    def * = (id, scoringKey, scoringType, label, parameters, description, position, category) <> (ScoringDefinition.tupled, ScoringDefinition.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(scoringKey), Rep.Some(scoringType), Rep.Some(label), parameters, Rep.Some(description), Rep.Some(position), Rep.Some(category)).shaped.<>({r=>import r._; _1.map(_=> ScoringDefinition.tupled((_1.get, _2.get, _3.get, _4.get, _5, _6.get, _7.get, _8.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[ScoringDefinitionId] = column[ScoringDefinitionId]("id", O.AutoInc, O.PrimaryKey)
    /** Database column scoring_key SqlType(varchar), Length(30,true) */
    val scoringKey: Rep[String] = column[String]("scoring_key", O.Length(30,varying=true))
    /** Database column scoring_type SqlType(varchar), Length(30,true) */
    val scoringType: Rep[io.towerstreet.slick.models.scoring.enums.ScoringType] = column[io.towerstreet.slick.models.scoring.enums.ScoringType]("scoring_type", O.Length(30,varying=true))
    /** Database column label SqlType(varchar) */
    val label: Rep[String] = column[String]("label")
    /** Database column parameters SqlType(json), Length(2147483647,false), Default(None) */
    val parameters: Rep[Option[play.api.libs.json.JsValue]] = column[Option[play.api.libs.json.JsValue]]("parameters", O.Length(2147483647,varying=false), O.Default(None))
    /** Database column description SqlType(varchar) */
    val description: Rep[String] = column[String]("description")
    /** Database column position SqlType(int4) */
    val position: Rep[Int] = column[Int]("position")
    /** Database column category SqlType(varchar), Length(20,true) */
    val category: Rep[io.towerstreet.slick.models.scoring.enums.ScoringCategory] = column[io.towerstreet.slick.models.scoring.enums.ScoringCategory]("category", O.Length(20,varying=true))

    /** Uniqueness Index over (scoringKey) (database name scoring_definition_scoring_key_key) */
    val index1 = index("scoring_definition_scoring_key_key", scoringKey, unique=true)
  }
  /** Collection-like TableQuery object for table ScoringDefinitionTable */
  lazy val ScoringDefinitionTable = new TableQuery(tag => new ScoringDefinitionTable(tag))

  /** Collection-like TableQuery object resolving inserted object's primary key for table OutcomeTaskResultTable */
  lazy val ScoringDefinitionTableInsert = ScoringDefinitionTable returning ScoringDefinitionTable.map(_.id) into ((item, id) => item.copy(id = id))
             

  /** Table description of table scoring_definition_with_template. Objects of this class serve as prototypes for rows in queries. */
  class ScoringDefinitionWithTemplateTable(_tableTag: Tag) extends profile.api.Table[ScoringDefinitionWithTemplate](_tableTag, Some("scoring"), "scoring_definition_with_template") {
    def * = (scoringDefinitionId, simulationTemplateId, label, description, position, category, scoringType) <> (ScoringDefinitionWithTemplate.tupled, ScoringDefinitionWithTemplate.unapply)

    /** Database column scoring_definition_id SqlType(int4), Default(None) */
    val scoringDefinitionId: Rep[Option[Int]] = column[Option[Int]]("scoring_definition_id", O.Default(None))
    /** Database column simulation_template_id SqlType(int4), Default(None) */
    val simulationTemplateId: Rep[Option[Int]] = column[Option[Int]]("simulation_template_id", O.Default(None))
    /** Database column label SqlType(varchar), Default(None) */
    val label: Rep[Option[String]] = column[Option[String]]("label", O.Default(None))
    /** Database column description SqlType(varchar), Default(None) */
    val description: Rep[Option[String]] = column[Option[String]]("description", O.Default(None))
    /** Database column position SqlType(int4), Default(None) */
    val position: Rep[Option[Int]] = column[Option[Int]]("position", O.Default(None))
    /** Database column category SqlType(varchar), Length(20,true), Default(None) */
    val category: Rep[Option[String]] = column[Option[String]]("category", O.Length(20,varying=true), O.Default(None))
    /** Database column scoring_type SqlType(varchar), Length(30,true), Default(None) */
    val scoringType: Rep[Option[String]] = column[Option[String]]("scoring_type", O.Length(30,varying=true), O.Default(None))
  }
  /** Collection-like TableQuery object for table ScoringDefinitionWithTemplateTable */
  lazy val ScoringDefinitionWithTemplateTable = new TableQuery(tag => new ScoringDefinitionWithTemplateTable(tag))

  /** Table description of table scoring_outcome. Objects of this class serve as prototypes for rows in queries. */
  class ScoringOutcomeTable(_tableTag: Tag) extends profile.api.Table[ScoringOutcome](_tableTag, Some("scoring"), "scoring_outcome") {
    def * = (id, customerAssessmentId, simulationOutcomeId, queuedAt, finishedAt, retries) <> (ScoringOutcome.tupled, ScoringOutcome.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(customerAssessmentId), simulationOutcomeId, Rep.Some(queuedAt), finishedAt, retries).shaped.<>({r=>import r._; _1.map(_=> ScoringOutcome.tupled((_1.get, _2.get, _3, _4.get, _5, _6)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[ScoringOutcomeId] = column[ScoringOutcomeId]("id", O.AutoInc, O.PrimaryKey)
    /** Database column customer_assessment_id SqlType(int4) */
    val customerAssessmentId: Rep[public.Model.CustomerAssessmentId] = column[public.Model.CustomerAssessmentId]("customer_assessment_id")
    /** Database column simulation_outcome_id SqlType(int4), Default(None) */
    val simulationOutcomeId: Rep[Option[attacksimulator.Model.SimulationOutcomeId]] = column[Option[attacksimulator.Model.SimulationOutcomeId]]("simulation_outcome_id", O.Default(None))
    /** Database column queued_at SqlType(timestamp) */
    val queuedAt: Rep[java.time.LocalDateTime] = column[java.time.LocalDateTime]("queued_at")
    /** Database column finished_at SqlType(timestamp), Default(None) */
    val finishedAt: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("finished_at", O.Default(None))
    /** Database column retries SqlType(int4), Default(None) */
    val retries: Rep[Option[Int]] = column[Option[Int]]("retries", O.Default(None))
  }
  /** Collection-like TableQuery object for table ScoringOutcomeTable */
  lazy val ScoringOutcomeTable = new TableQuery(tag => new ScoringOutcomeTable(tag))

  /** Collection-like TableQuery object resolving inserted object's primary key for table OutcomeTaskResultTable */
  lazy val ScoringOutcomeTableInsert = ScoringOutcomeTable returning ScoringOutcomeTable.map(_.id) into ((item, id) => item.copy(id = id))
             

  /** Table description of table scoring_result. Objects of this class serve as prototypes for rows in queries. */
  class ScoringResultTable(_tableTag: Tag) extends profile.api.Table[ScoringResult](_tableTag, Some("scoring"), "scoring_result") {
    def * = (id, scoringDefinitionId, scoringOutcomeId, createdAt, result, resultParameters) <> (ScoringResult.tupled, ScoringResult.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(scoringDefinitionId), Rep.Some(scoringOutcomeId), Rep.Some(createdAt), Rep.Some(result), resultParameters).shaped.<>({r=>import r._; _1.map(_=> ScoringResult.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[ScoringResultId] = column[ScoringResultId]("id", O.AutoInc, O.PrimaryKey)
    /** Database column scoring_definition_id SqlType(int4) */
    val scoringDefinitionId: Rep[ScoringDefinitionId] = column[ScoringDefinitionId]("scoring_definition_id")
    /** Database column scoring_outcome_id SqlType(int4) */
    val scoringOutcomeId: Rep[ScoringOutcomeId] = column[ScoringOutcomeId]("scoring_outcome_id")
    /** Database column created_at SqlType(timestamp) */
    val createdAt: Rep[java.time.LocalDateTime] = column[java.time.LocalDateTime]("created_at")
    /** Database column result SqlType(varchar), Length(30,true) */
    val result: Rep[io.towerstreet.slick.models.scoring.enums.ScoringResultType] = column[io.towerstreet.slick.models.scoring.enums.ScoringResultType]("result", O.Length(30,varying=true))
    /** Database column result_parameters SqlType(json), Length(2147483647,false), Default(None) */
    val resultParameters: Rep[Option[play.api.libs.json.JsValue]] = column[Option[play.api.libs.json.JsValue]]("result_parameters", O.Length(2147483647,varying=false), O.Default(None))
  }
  /** Collection-like TableQuery object for table ScoringResultTable */
  lazy val ScoringResultTable = new TableQuery(tag => new ScoringResultTable(tag))

  /** Collection-like TableQuery object resolving inserted object's primary key for table OutcomeTaskResultTable */
  lazy val ScoringResultTableInsert = ScoringResultTable returning ScoringResultTable.map(_.id) into ((item, id) => item.copy(id = id))
             

  /** Table description of table simulation_outcome_for_scoring. Objects of this class serve as prototypes for rows in queries. */
  class SimulationOutcomeForScoringTable(_tableTag: Tag) extends profile.api.Table[SimulationOutcomeForScoring](_tableTag, Some("scoring"), "simulation_outcome_for_scoring") {
    def * = (simulationOutcomeId, outcomeCreatedAt, outcomeFinishedAt, templateId, scoringOutcomeId, scoringQueuedAt, scoringFinishedAt, customerAssessmentId, retries) <> (SimulationOutcomeForScoring.tupled, SimulationOutcomeForScoring.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(simulationOutcomeId), Rep.Some(outcomeCreatedAt), outcomeFinishedAt, Rep.Some(templateId), scoringOutcomeId, scoringQueuedAt, scoringFinishedAt, Rep.Some(customerAssessmentId), retries).shaped.<>({r=>import r._; _1.map(_=> SimulationOutcomeForScoring.tupled((_1.get, _2.get, _3, _4.get, _5, _6, _7, _8.get, _9)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column simulation_outcome_id SqlType(int4) */
    val simulationOutcomeId: Rep[attacksimulator.Model.SimulationOutcomeId] = column[attacksimulator.Model.SimulationOutcomeId]("simulation_outcome_id")
    /** Database column outcome_created_at SqlType(timestamp) */
    val outcomeCreatedAt: Rep[java.time.LocalDateTime] = column[java.time.LocalDateTime]("outcome_created_at")
    /** Database column outcome_finished_at SqlType(timestamp), Default(None) */
    val outcomeFinishedAt: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("outcome_finished_at", O.Default(None))
    /** Database column template_id SqlType(int4) */
    val templateId: Rep[attacksimulator.Model.SimulationTemplateId] = column[attacksimulator.Model.SimulationTemplateId]("template_id")
    /** Database column scoring_outcome_id SqlType(int4), Default(None) */
    val scoringOutcomeId: Rep[Option[ScoringOutcomeId]] = column[Option[ScoringOutcomeId]]("scoring_outcome_id", O.Default(None))
    /** Database column scoring_queued_at SqlType(timestamp), Default(None) */
    val scoringQueuedAt: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("scoring_queued_at", O.Default(None))
    /** Database column scoring_finished_at SqlType(timestamp), Default(None) */
    val scoringFinishedAt: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("scoring_finished_at", O.Default(None))
    /** Database column customer_assessment_id SqlType(int4) */
    val customerAssessmentId: Rep[public.Model.CustomerAssessmentId] = column[public.Model.CustomerAssessmentId]("customer_assessment_id")
    /** Database column retries SqlType(int4), Default(None) */
    val retries: Rep[Option[Int]] = column[Option[Int]]("retries", O.Default(None))
  }
  /** Collection-like TableQuery object for table SimulationOutcomeForScoringTable */
  lazy val SimulationOutcomeForScoringTable = new TableQuery(tag => new SimulationOutcomeForScoringTable(tag))

  /** Table description of table simulation_scoring_config. Objects of this class serve as prototypes for rows in queries. */
  class SimulationScoringConfigTable(_tableTag: Tag) extends profile.api.Table[SimulationScoringConfig](_tableTag, Some("scoring"), "simulation_scoring_config") {
    def * = (scoringDefinitionId, simulationTemplateId, taskId) <> (SimulationScoringConfig.tupled, SimulationScoringConfig.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(scoringDefinitionId), Rep.Some(simulationTemplateId), Rep.Some(taskId)).shaped.<>({r=>import r._; _1.map(_=> SimulationScoringConfig.tupled((_1.get, _2.get, _3.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column scoring_definition_id SqlType(int4) */
    val scoringDefinitionId: Rep[ScoringDefinitionId] = column[ScoringDefinitionId]("scoring_definition_id")
    /** Database column simulation_template_id SqlType(int4) */
    val simulationTemplateId: Rep[attacksimulator.Model.SimulationTemplateId] = column[attacksimulator.Model.SimulationTemplateId]("simulation_template_id")
    /** Database column task_id SqlType(int4) */
    val taskId: Rep[attacksimulator.Model.TaskId] = column[attacksimulator.Model.TaskId]("task_id")
  }
  /** Collection-like TableQuery object for table SimulationScoringConfigTable */
  lazy val SimulationScoringConfigTable = new TableQuery(tag => new SimulationScoringConfigTable(tag))

  /** Table description of table simulation_scoring_result. Objects of this class serve as prototypes for rows in queries. */
  class SimulationScoringResultTable(_tableTag: Tag) extends profile.api.Table[SimulationScoringResult](_tableTag, Some("scoring"), "simulation_scoring_result") {
    def * = (id, scoringDefinitionId, simulationOutcomeId, scoringDefinitionKey, scoringDefinitionLabel, createdAt, result, resultParameters, simulationOutcomeToken, simulationOutcomeFinishedAt, scoringOutcomeFinishedAt) <> (SimulationScoringResult.tupled, SimulationScoringResult.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), scoringDefinitionId, simulationOutcomeId, scoringDefinitionKey, scoringDefinitionLabel, createdAt, result, resultParameters, simulationOutcomeToken, simulationOutcomeFinishedAt, scoringOutcomeFinishedAt).shaped.<>({r=>import r._; _1.map(_=> SimulationScoringResult.tupled((_1.get, _2, _3, _4, _5, _6, _7, _8, _9, _10, _11)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(int4) */
    val id: Rep[Int] = column[Int]("id")
    /** Database column scoring_definition_id SqlType(int4), Default(None) */
    val scoringDefinitionId: Rep[Option[Int]] = column[Option[Int]]("scoring_definition_id", O.Default(None))
    /** Database column simulation_outcome_id SqlType(int4), Default(None) */
    val simulationOutcomeId: Rep[Option[Int]] = column[Option[Int]]("simulation_outcome_id", O.Default(None))
    /** Database column scoring_definition_key SqlType(varchar), Length(30,true), Default(None) */
    val scoringDefinitionKey: Rep[Option[String]] = column[Option[String]]("scoring_definition_key", O.Length(30,varying=true), O.Default(None))
    /** Database column scoring_definition_label SqlType(varchar), Default(None) */
    val scoringDefinitionLabel: Rep[Option[String]] = column[Option[String]]("scoring_definition_label", O.Default(None))
    /** Database column created_at SqlType(timestamp), Default(None) */
    val createdAt: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("created_at", O.Default(None))
    /** Database column result SqlType(varchar), Length(30,true), Default(None) */
    val result: Rep[Option[String]] = column[Option[String]]("result", O.Length(30,varying=true), O.Default(None))
    /** Database column result_parameters SqlType(json), Length(2147483647,false), Default(None) */
    val resultParameters: Rep[Option[play.api.libs.json.JsValue]] = column[Option[play.api.libs.json.JsValue]]("result_parameters", O.Length(2147483647,varying=false), O.Default(None))
    /** Database column simulation_outcome_token SqlType(uuid), Default(None) */
    val simulationOutcomeToken: Rep[Option[java.util.UUID]] = column[Option[java.util.UUID]]("simulation_outcome_token", O.Default(None))
    /** Database column simulation_outcome_finished_at SqlType(timestamp), Default(None) */
    val simulationOutcomeFinishedAt: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("simulation_outcome_finished_at", O.Default(None))
    /** Database column scoring_outcome_finished_at SqlType(timestamp), Default(None) */
    val scoringOutcomeFinishedAt: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("scoring_outcome_finished_at", O.Default(None))
  }
  /** Collection-like TableQuery object for table SimulationScoringResultTable */
  lazy val SimulationScoringResultTable = new TableQuery(tag => new SimulationScoringResultTable(tag))
          
}
