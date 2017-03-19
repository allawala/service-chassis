name := """service-chassis"""

version := "1.0"

scalaVersion := "2.12.1"

scalacOptions ++= Seq(
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-feature", // Emit warning and location for usages of features that should be imported explicitly.
  "-unchecked", // Enable additional warnings where generated code depends on assumptions.
  "-Xfatal-warnings", // Fail the compilation if there are any warnings.
  "-Xlint", // Enable recommended additional warnings.
  "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver.
  "-Ywarn-dead-code", // Warn when dead code is identified.
  "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
  "-Ywarn-nullary-override", // Warn when non-nullary overrides nullary, e.g. def foo() over def foo.
  "-Ywarn-numeric-widen", // Warn when numerics are widened.
  "-target:jvm-1.8",
  "-encoding", "UTF-8",
  "-language:existentials",
  "-language:higherKinds"
)

libraryDependencies ++= {
  val akkaVersion = "2.4.17"
  val akkaHttpVersion = "10.0.5"
  val akkaHttpCirceVersion = "1.13.0"
  val circeVersion = "0.7.0"
  val ficusVersion = "1.4.0"
  val guiceVersion = "4.1.0"
  val logbackVersion = "1.2.2"
  val scalatestVersion = "3.0.1"

  Seq(
    // Akka
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,

    // Json
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion,
    "de.heikoseeberger" %% "akka-http-circe" % akkaHttpCirceVersion excludeAll ExclusionRule(organization = "io.circe"),

    // Logging
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "ch.qos.logback" % "logback-classic" % logbackVersion,

    // Config
    "com.iheart" %% "ficus" % ficusVersion,

    // DI
    "com.google.inject" % "guice" % guiceVersion,
    "net.codingwell" %% "scala-guice" % guiceVersion,

    // Test Dependencies
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion,
    "org.scalatest" %% "scalatest" % scalatestVersion % "test"
  )
}

initialCommands :=
  """
import akka.actor._
import akka.pattern._
import akka.util._
import scala.concurrent._
import scala.concurrent.duration._
  """.stripMargin

fork in run := true