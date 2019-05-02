package io.towerstreet.attacksimulator.dao

import io.towerstreet.dao.{AbstractDAO, DAOHelpers}
import io.towerstreet.slick.models.generated.attacksimulator.Model.{CampaignVisitorSimulation, SimulationOutcomeId}
import io.towerstreet.slick.models.generated.attacksimulator.Tables
import io.towerstreet.slick.models.generated.public.Model.CustomerId
import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.ExecutionContext

class CampaignDAO @Inject()(val dbConfigProvider: DatabaseConfigProvider)
                           (implicit ec: ExecutionContext)
  extends AbstractDAO
    with DAOHelpers
{

  import api._
  import implicits._

  def getVisitorSimulation(simulationOutcomeId: SimulationOutcomeId): DBIO[Option[CampaignVisitorSimulation]] = {
    Tables.CampaignVisitorSimulationTable
      .filter(_.simulationOutcomeId === simulationOutcomeId)
      .sortBy(_.simulationOutcomeCreatedAt.desc)
      .take(1)
      .result
      .headOption
  }

  def updateScoreHistogram(customerId: CustomerId, score: Int): DBIO[Int] = {
    /* Scoring percentile is implemented by maintaining histogram of score in the campaign. This is optimization which
     * significantly reduces load of DB. We don't need to query growing table to calculate counts but is enough to
     * check 10 rows. It has constant complexity on number of visitors vs linear.
     *
     * Note that customerId identifies special customer entity representing whole campaign.
     */
    sql"""INSERT INTO scoring.campaign_score_histogram(customer_id, score)
          VALUES ($customerId, $score)
          ON CONFLICT(customer_id, score) DO UPDATE SET value = campaign_score_histogram.value + 1
       """.asUpdate
  }

}
