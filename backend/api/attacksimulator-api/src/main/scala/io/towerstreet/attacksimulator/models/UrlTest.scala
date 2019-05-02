package io.towerstreet.attacksimulator.models

import com.github.vitalsoftware.macros.jsonDefaults

object UrlTest {
  @jsonDefaults
  case class UrlTaskParameters(url: String,
                               imagePath: Option[String] = None,
                               includeInUrlTesting: Boolean = true,
                               hasWebServer: Boolean = true,
                               hasImage: Boolean = true
                              )
}
