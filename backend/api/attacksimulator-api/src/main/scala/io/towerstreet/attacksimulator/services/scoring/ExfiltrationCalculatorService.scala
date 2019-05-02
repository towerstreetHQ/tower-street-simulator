package io.towerstreet.attacksimulator.services.scoring

import io.towerstreet.attacksimulator.models.ExfiltrationTest.ExfiltrationTaskParameters
import io.towerstreet.attacksimulator.models.Scoring.{ExfiltrationResultWitness, TaskWithResult}
import io.towerstreet.logging.Logging
import io.towerstreet.slick.models.generated.attacksimulator.Model.TaskId
import io.towerstreet.slick.models.generated.scoring.Model.{ScoringDefinition, ScoringOutcomeId, ScoringResult}
import javax.inject.Inject

import scala.concurrent.{ExecutionContext, Future}

/**
  * Calculation service to determine whether tested system intercepts uploaded data and prevent sensitive data loss.
  * Simulation tries to upload file containing sensitive data. Test is marked as success if upload failed.
  *
  * Test also tries to upload special empty files to verify that at least something can be uploaded. If empty file is
  * blocked then all other files are removed from calculation and result is inconclusive.
  */
class ExfiltrationCalculatorService @Inject()()(implicit ec: ExecutionContext)
  extends Logging
    with ScoringCalculationHelpers
{
  def calculateResult(scoringOutcomeId: ScoringOutcomeId,
                      scoringDefinition: ScoringDefinition,
                      taskOutcomes: Seq[(TaskId, Option[TaskWithResult])]
                     ): Future[ScoringResult] = {

    // Parse parameters and obtain tasks for test files and exfiltration files
    val (testFiles, tasks) = taskOutcomes.flatMap {
      case (id, Some(result)) =>
        val parameters = parseWithDefault(result.task.parameters, ExfiltrationTaskParameters())
        Some(id, result, parameters)

      // Not mapped tasks are excluded
      case (_, None) =>
        None
    }.partition(_._3.isTestFile)

    // Prepare collection of success empty test files used to verify results
    val successTestFiles = testFiles.filter(_._2.outcome.isSuccess).map(_._2.task.taskKey).toSet

    // Final results for each exfiltration file
    val witnesses: Seq[ExfiltrationResultWitness] = tasks.map {
      case (id, TaskWithResult(_, outcome, _), params) =>
        // If file should be verified by test file then check collection, otherwise just use it
        val testFileOk = params.testFileKey.fold(true)(successTestFiles.contains)

        ExfiltrationResultWitness(id, outcome.isSuccess, ! testFileOk, params.records, outcome.id, params.recordType)
    }

    evaluateCombinedResults(scoringOutcomeId, scoringDefinition, witnesses)(! _.ignored)(_.isSuccess)
  }

}
