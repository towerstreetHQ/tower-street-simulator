package io.towerstreet.attacksimulator.models

import com.github.vitalsoftware.macros.jsonDefaults
import io.towerstreet.slick.models.attacksimulator.enums.RecordType

object ExfiltrationTest {
  @jsonDefaults
  case class ExfiltrationTaskParameters(records: Int = 0,
                                        isTestFile: Boolean = false,
                                        testFileKey: Option[String] = None,
                                        recordType: Option[RecordType] = None
                                       )
}
