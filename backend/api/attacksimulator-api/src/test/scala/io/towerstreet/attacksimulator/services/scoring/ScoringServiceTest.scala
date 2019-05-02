package io.towerstreet.attacksimulator.services.scoring

import java.time.LocalDateTime
import java.util.UUID

import acolyte.jdbc.Implicits._
import acolyte.jdbc._
import io.towerstreet.attacksimulator.testhelpers.db.RowListTypes
import io.towerstreet.slick.models.generated.attacksimulator.Model.{SimulationOutcomeId, SimulationTemplateId, TaskId}
import io.towerstreet.slick.models.generated.public.Model.CustomerAssessmentId
import io.towerstreet.slick.models.generated.scoring.Model.SimulationOutcomeForScoring
import io.towerstreet.testhelpers.WithApplicationSpec.WithService
import io.towerstreet.testhelpers.db.ExecutorHandlers.{Handler, Param, Query, ~}
import io.towerstreet.testhelpers.db.{ResultHelpers, WithMockedDbApplication}
import org.scalatestplus.play.PlaySpec
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

import scala.collection.mutable

class ScoringServiceTest
  extends PlaySpec
    with FutureAwaits
    with DefaultAwaitTimeout
{
  private lazy val simulationDate = LocalDateTime.now().minusHours(1)
  private lazy val outcomeDate = LocalDateTime.now().minusHours(1)
  private lazy val outcomesForScoringResults = (RowListTypes.SimulationOutcomeForScoringRowType
      :+ (1, simulationDate, simulationDate, 1, 1, outcomeDate, null, 1, null)
      :+ (2, simulationDate, simulationDate, 1, null, null, null, 2, null)
    ).asResult()

  private lazy val campaignVisitor = UUID.fromString("e9d9396c-0d4b-4f34-ae53-07f85c68b07b")
  private lazy val now = LocalDateTime.now()

  class WithSpecApplication(outcomesForScoring: QueryResult = outcomesForScoringResults)
    extends WithMockedDbApplication
      with WithService[ScoringService] {

    val insertedOutcomes = mutable.ArrayBuffer.empty[Seq[Any]]
    val insertedResults = mutable.ArrayBuffer.empty[Seq[Any]]
    val updatedOutcomes = mutable.ArrayBuffer.empty[Seq[Any]]
    val updatedHistogram = mutable.ArrayBuffer.empty[Seq[Any]]

    override lazy val handler: StatementHandler = Handler("^select ") {
      case ~(Query("""^select .* from "scoring"."simulation_outcome_for_scoring"""")) => outcomesForScoring


      case ~(Query("""^select .* from "scoring"."scoring_outcome".*"simulation_outcome_id" = 1""")) =>
        (RowListTypes.ScoringOutcomeRowType :+ (1, 1, 1, outcomeDate, null, null)).asResult()
      case ~(Query("""^select .* from "scoring"."scoring_outcome".*"simulation_outcome_id" = 2""")) =>
        (RowListTypes.ScoringOutcomeRowType :+ (2, 2, 2, outcomeDate, null, null)).asResult()


      case ~(Query("""^select .* from "attacksimulator"."outcome_task_result".*"simulation_outcome_id" = 1""")) =>
        (RowListTypes.TasksWithResultsRowType
          :+ (1, 1, 1, true, "task-01-outcome", null, null, null, 1, 1, "task-01", null, null, null, null, null, null, null, null, null)
          :+ (2, 1, 2, true, "task-02-outcome", null, null, null, 2, 1, "task-02", null, null, null, null, null, null, null, null, null)
          :+ (3, 1, 3, true, "task-03-outcome", null, null, null, 3, 1, "task-03", null, null, null, null, null, null, null, null, null)
          :+ (4, 1, 4, false, "task-04-outcome", null, null, null, 4, 1, "task-04", null, null, null, null, null, null, null, null, null)
        ).asResult()
      case ~(Query("""^select .* from "attacksimulator"."outcome_task_result".*"simulation_outcome_id" = 2""")) =>
        QueryResult.Nil


      case ~(Query("""^select .* from "scoring"."simulation_scoring_config".*"simulation_template_id" = 1""")) =>
        (RowListTypes.SimulationScoringConfigRowType
          :+ (1, 1, 1)
          :+ (1, 1, 2)
          :+ (1, 1, 3)
          :+ (2, 1, 4)
          :+ (2, 1, 5)
        ).asResult()
      case ~(Query("""^select .* from "scoring"."simulation_scoring_config".*"simulation_template_id" = 2""")) =>
        QueryResult.Nil


      case ~(Param("""^select .* from "scoring"."scoring_definition""""), (1, 2)) =>
        (RowListTypes.ScoringDefinitionRowType
          :+ (1, "scoring-01", "boolean-result", "", null, "", 1, "cis")
          :+ (2, "scoring-02", "boolean-result", "", null, "", 2, "cis")
        ).asResult()
      case ~(Query("""^select .* from "scoring"."scoring_definition"""")) =>
        QueryResult.Nil


      case ~(Query("""^select .* from "attacksimulator"."campaign_visitor_simulation"""")) =>
        (RowListTypes.CampaignVisitorSimulationRowType :+ (1, 1, 1, campaignVisitor, 1, 1, now, now, now, 1, now, now, true, 1)).asResult


      case ~(Param("""^insert into "scoring"."scoring_outcome""""), v@_*) => {
        insertedOutcomes += v
        ResultHelpers.intKeyResult(v(1).asInstanceOf[Int])
      }

      case ~(Param("""^UPDATE "scoring"."scoring_outcome""""), v@_*) => {
        updatedOutcomes += v
        UpdateResult.One
      }

      case ~(Param("""^update "scoring"."scoring_outcome""""), v@_*) => {
        updatedOutcomes += v
        UpdateResult.One
      }

      case ~(Param("""^insert into "scoring"."scoring_result""""), v@_*) => {
        insertedResults += v
        ResultHelpers.intKeyResult(v(0).asInstanceOf[Int])
      }

      case ~(Param("""^INSERT INTO scoring.campaign_score_histogram.*"""), v@_*) => {
        updatedHistogram += v
        ResultHelpers.intKeyResult(v(0).asInstanceOf[Int])
      }
    }
  }

  "ScoringService" should {
    "loadSimulationOutcomesForScoring" should {

      "load simulation outcomes and create scoring outcomes" in new WithSpecApplication {
        val r = await(service.loadSimulationOutcomesForScoring())

        r must have size 2
        insertedOutcomes must have size 1
        updatedOutcomes must have size 1
      }

      "do nothing if there are no results" in new WithSpecApplication(QueryResult.Nil) {
        val r = await(service.loadSimulationOutcomesForScoring())

        r must have size 0
        insertedOutcomes must have size 0
        updatedOutcomes must have size 0
      }

    }

    "getScoringDefinitionsWithOutcome" should {
      "load task results and scoring definitions for simulation outcome" in new WithSpecApplication {
        val r = await(service.getScoringDefinitionsWithOutcome(SimulationOutcomeId(1), SimulationTemplateId(1)))

        r.scoringDefinitions must have size 2
        r.scoringDefinitions(0).config.map(_.taskId) mustBe Seq(TaskId(1), TaskId(2), TaskId(3))
        r.scoringDefinitions(1).config.map(_.taskId) mustBe Seq(TaskId(4), TaskId(5))
      }

      "load no definitions if there is no definition for template" in new WithSpecApplication {
        val r = await(service.getScoringDefinitionsWithOutcome(SimulationOutcomeId(2), SimulationTemplateId(2)))

        r.scoringDefinitions mustBe empty
      }
    }

    "performScoring" should {
      "retrieve data from DB, calculate results and persist them to DB" in new WithSpecApplication {
        val outcomeForScoring = SimulationOutcomeForScoring(SimulationOutcomeId(1), simulationDate, Some(simulationDate),
          SimulationTemplateId(1), customerAssessmentId = CustomerAssessmentId(1))

        val r = await(service.performScoring(outcomeForScoring))

        insertedResults must have size 2
        updatedOutcomes must have size 1
        updatedHistogram must have size 1
      }

      "do nothing if there are no scoring definitions" in new WithSpecApplication {
        val outcomeForScoring = SimulationOutcomeForScoring(SimulationOutcomeId(2), simulationDate, Some(simulationDate),
          SimulationTemplateId(2), customerAssessmentId = CustomerAssessmentId(2))

        val r = await(service.performScoring(outcomeForScoring))

        insertedResults mustBe empty
        updatedOutcomes must have size 1
        updatedHistogram must have size 1
      }
    }
  }
}
