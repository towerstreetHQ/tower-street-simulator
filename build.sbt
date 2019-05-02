
lazy val defaultSettings = Seq(
  organization := Constants.TowerStreetOrganization,
  version := Constants.TowerStreetVersion,
  scalaVersion := Constants.ScalaVersion,
  test in assembly := {},
  scalacOptions += "-Ypartial-unification",
  credentials += sys.env.get("JENKINS_ARTIFACTORY").map(password =>
      Credentials("Artifactory Realm", "ceai.jfrog.io", "jenkins", password)
    ).getOrElse(
      Credentials(Path.userHome / ".ivy2" / ".credentials")
    )
)

lazy val slickModelSettings = Seq(
  name := "slick-model",
  parallelExecution in Test := false,
  fork in Test := true,
  libraryDependencies ++= Dependencies.SlickModel.Dependencies ++ Dependencies.SlickModel.TestDependencies,
)

lazy val slickModel = (project in file("backend/commons/slick-model")).
  settings(defaultSettings, slickModelSettings)

lazy val playApiHelpersSettings = Seq(
  name := "play-api-helpers",
  parallelExecution in Test := false,
  fork in Test := true,
  libraryDependencies ++= Dependencies.PlayApiHelpers.Dependencies ++ Dependencies.PlayApiHelpers.TestDependencies,

  // Resolving evictions
  dependencyOverrides += Dependencies.scalaParserCombinators,
)

lazy val playApiHelpers = (project in file("backend/commons/play-api-helpers")).
  settings(defaultSettings, playApiHelpersSettings).
  dependsOn(slickModel)

lazy val playApiSharedSettings = Seq(
  // Enabling custom structure for play file so we can have src/main/scala folder structure and
  // not one required by play
  PlayKeys.playMonitoredFiles ++= (sourceDirectories in (Compile)).value,

  // Extra play dependency for guice from play plugin
  libraryDependencies += guice,

  // Build info properties - creates object with versions from SBT
  buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),

  autoCompilerPlugins := true,
  addCompilerPlugin("org.eu.acolyte" %% "scalac-plugin" % "1.0.49"),

  assemblyMergeStrategy in assembly := {
    case "secret.conf" => MergeStrategy.discard
    case "play/reference-overrides.conf" => MergeStrategy.last
    case PathList("org", "cyberneko", xs @ _*) => MergeStrategy.last
    case PathList("org", "slf4j", xs @ _*) => MergeStrategy.last
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  },

  // Disable warnings for 3rd party evictions - will keep our build clean and it is not problem for us
  // There were huge number of warnings because play includes different versions of libraries in different transitive
  // dependencies. It can be handled by dependencyOverrides property but we would need to set long list of overrides.
  // This would also make problems for migrations to higher version of play libraries - we would need to manually update
  // each dependency.
  evictionWarningOptions in update := EvictionWarningOptions.default
    .withWarnTransitiveEvictions(false)
    .withWarnDirectEvictions(true)
)


lazy val attacksimulatorApiSettings = Seq(
  name := "attacksimulator-api",
  parallelExecution in Test := false,
  fork in Test := true,
  libraryDependencies ++= Dependencies.AttacksimulatorApi.Dependencies ++ Dependencies.AttacksimulatorApi.TestDependencies,

  PlayKeys.devSettings += "play.server.http.port" -> "9001",
  buildInfoPackage := "io.towerstreet.attacksimulator",
  assemblyJarName in assembly := s"attacksimulator-api.jar",
)

lazy val attacksimulatorApi = (project in file("backend/api/attacksimulator-api")).
  settings(defaultSettings, attacksimulatorApiSettings, playApiSharedSettings).
  dependsOn(playApiHelpers % "compile->compile;test->test").
  enablePlugins(PlayScala, BuildInfoPlugin).
  disablePlugins(PlayLayoutPlugin)
