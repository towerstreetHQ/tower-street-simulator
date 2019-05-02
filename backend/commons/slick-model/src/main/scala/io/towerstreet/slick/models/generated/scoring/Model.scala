
package io.towerstreet.slick.models.generated.scoring

import shapeless.tag.@@
import shapeless.tag
import slick.collection.heterogeneous._
import slick.collection.heterogeneous.syntax.HNil
import io.towerstreet.slick.models.generated._

object Model {

  /** Entity class storing rows of table CampaignScoreHistogramTable
   *  @param customerId Database column customer_id SqlType(int4)
   *  @param score Database column score SqlType(int4)
   *  @param value Database column value SqlType(int4), Default(1) */
  case class CampaignScoreHistogram(customerId: public.Model.CustomerId, score: Int, value: Int = 1)
  
  
  trait ScoringDefinitionTag
  type ScoringDefinitionId = Int @@ ScoringDefinitionTag
  
  object ScoringDefinitionId {
    def apply(id: Int): ScoringDefinitionId = tag[ScoringDefinitionTag][Int](id)
  }
             
  /** Entity class storing rows of table ScoringDefinitionTable
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param scoringKey Database column scoring_key SqlType(varchar), Length(30,true)
   *  @param scoringType Database column scoring_type SqlType(varchar), Length(30,true)
   *  @param label Database column label SqlType(varchar)
   *  @param parameters Database column parameters SqlType(json), Length(2147483647,false), Default(None)
   *  @param description Database column description SqlType(varchar)
   *  @param position Database column position SqlType(int4)
   *  @param category Database column category SqlType(varchar), Length(20,true) */
  case class ScoringDefinition(id: ScoringDefinitionId, scoringKey: String, scoringType: io.towerstreet.slick.models.scoring.enums.ScoringType, label: String, parameters: Option[play.api.libs.json.JsValue] = None, description: String, position: Int, category: io.towerstreet.slick.models.scoring.enums.ScoringCategory)
  
  /** Entity class storing rows of table ScoringDefinitionWithTemplateTable
   *  @param scoringDefinitionId Database column scoring_definition_id SqlType(int4), Default(None)
   *  @param simulationTemplateId Database column simulation_template_id SqlType(int4), Default(None)
   *  @param label Database column label SqlType(varchar), Default(None)
   *  @param description Database column description SqlType(varchar), Default(None)
   *  @param position Database column position SqlType(int4), Default(None)
   *  @param category Database column category SqlType(varchar), Length(20,true), Default(None)
   *  @param scoringType Database column scoring_type SqlType(varchar), Length(30,true), Default(None) */
  case class ScoringDefinitionWithTemplate(scoringDefinitionId: Option[Int] = None, simulationTemplateId: Option[Int] = None, label: Option[String] = None, description: Option[String] = None, position: Option[Int] = None, category: Option[String] = None, scoringType: Option[String] = None)
  
  
  trait ScoringOutcomeTag
  type ScoringOutcomeId = Int @@ ScoringOutcomeTag
  
  object ScoringOutcomeId {
    def apply(id: Int): ScoringOutcomeId = tag[ScoringOutcomeTag][Int](id)
  }
             
  /** Entity class storing rows of table ScoringOutcomeTable
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param customerAssessmentId Database column customer_assessment_id SqlType(int4)
   *  @param simulationOutcomeId Database column simulation_outcome_id SqlType(int4), Default(None)
   *  @param queuedAt Database column queued_at SqlType(timestamp)
   *  @param finishedAt Database column finished_at SqlType(timestamp), Default(None)
   *  @param retries Database column retries SqlType(int4), Default(None) */
  case class ScoringOutcome(id: ScoringOutcomeId, customerAssessmentId: public.Model.CustomerAssessmentId, simulationOutcomeId: Option[attacksimulator.Model.SimulationOutcomeId] = None, queuedAt: java.time.LocalDateTime, finishedAt: Option[java.time.LocalDateTime] = None, retries: Option[Int] = None)
  
  
  trait ScoringResultTag
  type ScoringResultId = Int @@ ScoringResultTag
  
  object ScoringResultId {
    def apply(id: Int): ScoringResultId = tag[ScoringResultTag][Int](id)
  }
             
  /** Entity class storing rows of table ScoringResultTable
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param scoringDefinitionId Database column scoring_definition_id SqlType(int4)
   *  @param scoringOutcomeId Database column scoring_outcome_id SqlType(int4)
   *  @param createdAt Database column created_at SqlType(timestamp)
   *  @param result Database column result SqlType(varchar), Length(30,true)
   *  @param resultParameters Database column result_parameters SqlType(json), Length(2147483647,false), Default(None) */
  case class ScoringResult(id: ScoringResultId, scoringDefinitionId: ScoringDefinitionId, scoringOutcomeId: ScoringOutcomeId, createdAt: java.time.LocalDateTime, result: io.towerstreet.slick.models.scoring.enums.ScoringResultType, resultParameters: Option[play.api.libs.json.JsValue] = None)
  
  /** Entity class storing rows of table SimulationOutcomeForScoringTable
   *  @param simulationOutcomeId Database column simulation_outcome_id SqlType(int4)
   *  @param outcomeCreatedAt Database column outcome_created_at SqlType(timestamp)
   *  @param outcomeFinishedAt Database column outcome_finished_at SqlType(timestamp), Default(None)
   *  @param templateId Database column template_id SqlType(int4)
   *  @param scoringOutcomeId Database column scoring_outcome_id SqlType(int4), Default(None)
   *  @param scoringQueuedAt Database column scoring_queued_at SqlType(timestamp), Default(None)
   *  @param scoringFinishedAt Database column scoring_finished_at SqlType(timestamp), Default(None)
   *  @param customerAssessmentId Database column customer_assessment_id SqlType(int4)
   *  @param retries Database column retries SqlType(int4), Default(None) */
  case class SimulationOutcomeForScoring(simulationOutcomeId: attacksimulator.Model.SimulationOutcomeId, outcomeCreatedAt: java.time.LocalDateTime, outcomeFinishedAt: Option[java.time.LocalDateTime] = None, templateId: attacksimulator.Model.SimulationTemplateId, scoringOutcomeId: Option[ScoringOutcomeId] = None, scoringQueuedAt: Option[java.time.LocalDateTime] = None, scoringFinishedAt: Option[java.time.LocalDateTime] = None, customerAssessmentId: public.Model.CustomerAssessmentId, retries: Option[Int] = None)
  
  /** Entity class storing rows of table SimulationScoringConfigTable
   *  @param scoringDefinitionId Database column scoring_definition_id SqlType(int4)
   *  @param simulationTemplateId Database column simulation_template_id SqlType(int4)
   *  @param taskId Database column task_id SqlType(int4) */
  case class SimulationScoringConfig(scoringDefinitionId: ScoringDefinitionId, simulationTemplateId: attacksimulator.Model.SimulationTemplateId, taskId: attacksimulator.Model.TaskId)
  
  /** Entity class storing rows of table SimulationScoringResultTable
   *  @param id Database column id SqlType(int4)
   *  @param scoringDefinitionId Database column scoring_definition_id SqlType(int4), Default(None)
   *  @param simulationOutcomeId Database column simulation_outcome_id SqlType(int4), Default(None)
   *  @param scoringDefinitionKey Database column scoring_definition_key SqlType(varchar), Length(30,true), Default(None)
   *  @param scoringDefinitionLabel Database column scoring_definition_label SqlType(varchar), Default(None)
   *  @param createdAt Database column created_at SqlType(timestamp), Default(None)
   *  @param result Database column result SqlType(varchar), Length(30,true), Default(None)
   *  @param resultParameters Database column result_parameters SqlType(json), Length(2147483647,false), Default(None)
   *  @param simulationOutcomeToken Database column simulation_outcome_token SqlType(uuid), Default(None)
   *  @param simulationOutcomeFinishedAt Database column simulation_outcome_finished_at SqlType(timestamp), Default(None)
   *  @param scoringOutcomeFinishedAt Database column scoring_outcome_finished_at SqlType(timestamp), Default(None) */
  case class SimulationScoringResult(id: Int, scoringDefinitionId: Option[Int] = None, simulationOutcomeId: Option[Int] = None, scoringDefinitionKey: Option[String] = None, scoringDefinitionLabel: Option[String] = None, createdAt: Option[java.time.LocalDateTime] = None, result: Option[String] = None, resultParameters: Option[play.api.libs.json.JsValue] = None, simulationOutcomeToken: Option[java.util.UUID] = None, simulationOutcomeFinishedAt: Option[java.time.LocalDateTime] = None, scoringOutcomeFinishedAt: Option[java.time.LocalDateTime] = None)
}
        
