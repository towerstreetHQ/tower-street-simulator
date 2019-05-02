package io.towerstreet.attacksimulator.models

import com.github.vitalsoftware.macros.json

object TestSinks {
  @json
  case class FileSinkResponse(fileName: String,
                              contentType: Option[String],
                              fileSize: Long
                             )
}
