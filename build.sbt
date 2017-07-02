name := """service-chassis"""

organization := "allawala"

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
  val akkaHttpCorsVersion = "0.2.1"
  val akkaHttpVersion = "10.0.5"
  val akkaHttpCirceVersion = "1.13.0"
  val circeVersion = "0.7.0"
  val enumeratumVersion = "1.5.10"
  val ficusVersion = "1.4.0"
  val groovyVersion = "2.4.10"
  val guiceVersion = "4.1.0"
  val jwtCirceVersion = "0.12.1"
  val logbackVersion = "1.2.2"
  val logstashVersion = "4.9"
  val metricsVersion = "3.5.6_a2.4"
  val scalaLoggingVersion = "3.5.0"
  val scalatestVersion = "3.0.1"
  val slf4jVersion = "1.7.25"
  val shiroVersion = "1.3.2"

  Seq(
    // Akka
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "ch.megard" %% "akka-http-cors" % akkaHttpCorsVersion,

    // Config
    "com.iheart" %% "ficus" % ficusVersion,

    // DI
    "com.google.inject" % "guice" % guiceVersion,
    "net.codingwell" %% "scala-guice" % guiceVersion,

    // Enums
    "com.beachape" %% "enumeratum" % enumeratumVersion,
    "com.beachape" %% "enumeratum-circe" % enumeratumVersion,

    // Json
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion,
    "io.circe" %% "circe-optics" % circeVersion,
    "io.circe" %% "circe-generic-extras" % circeVersion,

    "de.heikoseeberger" %% "akka-http-circe" % akkaHttpCirceVersion excludeAll ExclusionRule(organization = "io.circe"),

    // JWT
    "com.pauldijou" %% "jwt-circe" % jwtCirceVersion,

    // Logging
    "org.codehaus.groovy" % "groovy-all" % groovyVersion, // To allow log config to be defined in groovy
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "ch.qos.logback" % "logback-classic" % logbackVersion,
    "net.logstash.logback" % "logstash-logback-encoder" % logstashVersion,
    "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion,
    "org.slf4j" % "jcl-over-slf4j" % slf4jVersion,
    "org.slf4j" % "log4j-over-slf4j" % slf4jVersion,

    // Metrics
    "nl.grons" %% "metrics-scala" % metricsVersion,

    // Shiro
    "org.apache.shiro" % "shiro-core" % shiroVersion,
    "org.apache.shiro" % "shiro-guice" % shiroVersion,

    // Test Dependencies
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test",
    "org.scalatest" %% "scalatest" % scalatestVersion % "test"
  )
}

// plugins
enablePlugins(BuildInfoPlugin, GitVersioning, GitBranchPrompt, DockerPlugin)

// BuildInfo plugin Settings
buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, git.gitCurrentBranch, git.gitHeadCommit)
buildInfoPackage := "allawala"
buildInfoOptions += BuildInfoOption.BuildTime

// sbt git plugin settings
// turn on the version detection
git.useGitDescribe := true
git.baseVersion := "1.0.0"

// publish info
publishMavenStyle := true
publishTo := {
  if (version.value.endsWith("SNAPSHOT")) {
    Some("Service Chassis Snapshots" at "s3://s3-ap-southeast-2.amazonaws.com/maven.allawala.com/service-chassis/snapshots")
  } else {
    Some("Service Chassis Snapshots" at "s3://s3-ap-southeast-2.amazonaws.com/maven.allawala.com/service-chassis/releases")
  }
}

lazy val removeDangling = taskKey[Unit]("Task to clean up dangling docker images")

removeDangling := {
  // TODO get the dockerpath from env like ${DOCKER_HOME}
  val dockerPath = "/usr/local/bin/docker"
  val findDangling = s"$dockerPath images -f dangling=true -q"
  val removeDangling = s"xargs $dockerPath rmi -f"

  val deleted = findDangling.#|(removeDangling).!!

  val s: TaskStreams = streams.value
  if (deleted.isEmpty) {
    s.log.info("No dangling images to delete")
  } else {
    s.log.info(s"The following dangling images were deleted\n$deleted")
  }
}

// docker
// TODO review these options
buildOptions in docker := BuildOptions(
  //  cache = false,
  removeIntermediateContainers = BuildOptions.Remove.Always
  //  pullBaseImage = BuildOptions.Pull.Always
)

imageNames in docker := {
  val registry = "xxxxx.dkr.ecr.eu-central-1.amazonaws.com/xxxxx"

  def withSha: ImageName = {
    val tag = for {
      sha <- git.gitHeadCommit.value
      v = version.value
    } yield s"$v-$sha"

    ImageName(
      registry = Some(registry),
      repository = name.value,
      tag = tag
    )
  }

  def withoutSha: ImageName = {
    ImageName(
      registry = Some(registry),
      repository = name.value,
      tag = Some(version.value)
    )
  }

  def latest: ImageName = ImageName(
    registry = Some(organization.value),
    repository = name.value,
    tag = Some("latest")
  )

  // This assumes a multibranch pipeline structure and the presence of BRANCH_NAME env variable
  val imageName = sys.env.get("BRANCH_NAME").map(_.toLowerCase) match {
    case Some("develop") => withSha
    case Some("master") => withoutSha
    case _ =>
      latest
  }

  Seq(imageName)
}

dockerfile in docker := {
  val exposePort = 8080
  // The assembly task generates a fat JAR file
  val artifact: File = assembly.value
  val artifactTargetPath = s"/opt/${name.value}/${artifact.name}"

  /*
  Using the hseeberger/scala-sbt image as a quick start. This is not tagged and can change plus it uses openjdk so this is not
  a production ready docker image
  */
  new Dockerfile {
    from("hseeberger/scala-sbt")
    expose(exposePort)
    copy(artifact, artifactTargetPath)
    entryPoint("java", "-jar", artifactTargetPath)
  }
}

// releasing with the gitflow plugin
import sbtrelease._
import com.servicerocket.sbt.release.git.flow.Steps._
import sbtrelease.ReleaseStateTransformations._

releaseProcess := Seq(
  releaseStepCommand(ExtraReleaseCommands.initialVcsChecksCommand),
  checkSnapshotDependencies,
  checkGitFlowExists,
  inquireVersions,
  runTest,
  gitFlowReleaseStart,
  setReleaseVersion,
  commitReleaseVersion,
  publishArtifacts,
  gitFlowReleaseFinish,
  pushMaster,
  setNextVersion,
  commitNextVersion,
  pushChanges
)

mappings in (Compile, packageBin) ~= { seq =>
  seq.filter{
    case (file, _) =>
      val fileName = file.getName
      !(fileName.equalsIgnoreCase("logback.groovy") || fileName.startsWith("BuildInfo"))
  }
}

// Dependency tree
dependencyDotFile := file("dependencies.dot") //render dot file to `./dependencies.dot`

initialCommands :=
  """
import akka.actor._
import akka.pattern._
import akka.util._
import scala.concurrent._
import scala.concurrent.duration._
  """.stripMargin

fork in run := true