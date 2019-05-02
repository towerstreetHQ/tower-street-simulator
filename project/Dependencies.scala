import sbt._

object Dependencies {

  val slickVersion = "3.2.3"
  val slickPgVersion = "0.16.3"
  val enumeratumVersion = "1.5.13"
  val playVersion = "2.6.20"

  val shapeless = "com.chuusai" %% "shapeless" % "2.3.3"
  val playSlick = "com.typesafe.play" %% "play-slick" % "3.0.3"
  val jsonAnnotation = "com.github.vital-software" %% "json-annotation" % "0.6.0"
  val bcrypt = "com.github.t3hnar" %% "scala-bcrypt" % "3.1"
  val playFlyway = "org.flywaydb" %% "flyway-play" % "5.2.0"

  val enumeratum = "com.beachape" %% "enumeratum" % enumeratumVersion
  val enumeratumSlick = "com.beachape" %% "enumeratum-slick" % "1.5.15" // For some reason version is +2 from enumeratum
  val enumeratumPlayJson = "com.beachape" %% "enumeratum-play-json" % "1.5.14"

  val slick = "com.typesafe.slick" %% "slick" % slickVersion

  val slickPg = "com.github.tminglei" %% "slick-pg" % slickPgVersion
  val slickPgJson = "com.github.tminglei" %% "slick-pg_play-json" % slickPgVersion

  val apacheCommonsNet = "commons-net" % "commons-net" % "3.6"

  val playGuice = ("com.typesafe.play" %% "play-guice" % playVersion)
    .exclude("com.google.guava", "guava")

  val amazonAwsS3 = "com.amazonaws" % "aws-java-sdk-s3" % "1.11.434" exclude("commons-logging", "commons-logging")

  val scalaParserCombinators = "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.6"

  object Test {
    val playTests = "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % "test"
    val scalaTests = "org.scalatest" %% "scalatest" % "3.0.5" % "test"
    val acolyte = "org.eu.acolyte" %% "jdbc-scala" % "1.0.49" % "test"
    val akkaTestkit =  "com.typesafe.akka" %% "akka-testkit" % "2.5.17" % "test"   // Has to align with Akka version used by Play
  }


  object SlickModel {
    val Dependencies = Seq(shapeless, slick, slickPg, slickPgJson, enumeratum, enumeratumSlick, enumeratumPlayJson)
    val TestDependencies = Seq(Test.scalaTests)
  }

  object PlayApiHelpers {
    val Dependencies = Seq(playSlick, bcrypt, playFlyway, playGuice, amazonAwsS3)
    val TestDependencies = Seq(Test.playTests, Test.acolyte, Test.akkaTestkit)
  }

  object AttacksimulatorApi {
    val Dependencies = Seq(jsonAnnotation, apacheCommonsNet)
    val TestDependencies = Seq()
  }
}
