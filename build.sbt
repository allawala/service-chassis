import sbt.Keys.*
import sbt.*
import sbtbuildinfo.BuildInfoKeys.{buildInfoKeys, buildInfoOptions, buildInfoPackage}
import sbtbuildinfo.{BuildInfoKey, BuildInfoOption}

import scala.sys.process.*

name := """service-chassis"""

organization := "allawala"

scalaVersion := "2.13.15"

scalacOptions ++= Seq(
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-feature", // Emit warning and location for usages of features that should be imported explicitly.
  "-unchecked", // Enable additional warnings where generated code depends on assumptions.
  "-Xfatal-warnings", // Fail the compilation if there are any warnings.
  "-Xlint", // Enable recommended additional warnings.
  "-Xlint:-byname-implicit", // To disable Block result was adapted via implicit conversion (method apply) taking a by-name parameter
  "-Ywarn-dead-code", // Warn when dead code is identified.
  "-Ywarn-numeric-widen", // Warn when numerics are widened.
  "-release:21",
  "-encoding", "UTF-8",
  "-language:existentials",
  "-language:higherKinds"
)

libraryDependencies ++= {
  val akkaVersion = "2.9.5"
  val akkaHttpVersion = "10.6.3"
  val akkaHttpCirceVersion = "1.39.2"
  val beanUtilsVersion = "1.9.4"
  val bouncyCastleVersion = "1.78.1"
  val circeVersion = "0.14.10"
  val circeOpticsVersion = "0.15.0"
  val circeGenericsVersion = "0.14.4"
  val enumeratumVersion = "1.7.5"
  val enumeratumCirceVersion = "1.7.5"
  val ficusVersion = "1.5.2"
  val guiceVersion = "7.0.0"
  val scalaGuiceVersion = "7.0.0"
  val jakartaXmlBindVersion = "4.0.2"
  val jwtCirceVersion = "10.0.1"
  val logbackVersion = "1.5.8"
  val metricsVersion = "4.3.2"
  val mockitoVersion = "5.14.1"
  val scalaI18nVersion = "1.1.0"
  val scalaLoggingVersion = "3.9.5"
  val scalatestVersion = "3.2.19"
  val scalatestMockitoVersion = "3.2.19.0"
  val shiroVersion = "1.13.0"
  val threeTenExtraVersion = "1.8.0"

  Seq(
    // Akka
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-pki" % akkaVersion,

    // BeanUtils
    "commons-beanutils" % "commons-beanutils" % beanUtilsVersion,

    // BouncyCastle
    "org.bouncycastle" % "bcprov-jdk15to18" % bouncyCastleVersion,
    "org.bouncycastle" % "bcpkix-jdk15to18" % bouncyCastleVersion,

    // Config
    "com.iheart" %% "ficus" % ficusVersion,

    // DI
    "com.google.inject" % "guice" % guiceVersion,
    "net.codingwell" %% "scala-guice" % scalaGuiceVersion,

    // Enums
    "com.beachape" %% "enumeratum" % enumeratumVersion,
    "com.beachape" %% "enumeratum-circe" % enumeratumCirceVersion,

    // Json
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion,
    "io.circe" %% "circe-optics" % circeOpticsVersion,
    "io.circe" %% "circe-generic-extras" % circeGenericsVersion,

    "de.heikoseeberger" %% "akka-http-circe" % akkaHttpCirceVersion excludeAll ExclusionRule(organization = "io.circe"),

    // JWT
    "com.github.jwt-scala" %% "jwt-circe" % jwtCirceVersion,

    "jakarta.xml.bind" % "jakarta.xml.bind-api" % jakartaXmlBindVersion excludeAll ExclusionRule(organization = "jakarta.activation"),

    // Logging
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "ch.qos.logback" % "logback-classic" % logbackVersion,
    "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion,

    // Metrics
    "nl.grons" %% "metrics4-scala" % metricsVersion,

    // Shiro
    "org.apache.shiro" % "shiro-core" % shiroVersion,
    "org.apache.shiro" % "shiro-guice" % shiroVersion excludeAll ExclusionRule(organization = "com.google.inject.extensions"),

    // Threeten Extras
    "org.threeten" % "threeten-extra" % threeTenExtraVersion,

    // i18n
    "com.osinka.i18n" %% "scala-i18n" % scalaI18nVersion,

    // Test Dependencies
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test",
    "org.mockito" % "mockito-core" % mockitoVersion % "test",
    "org.scalatest" %% "scalatest" % scalatestVersion % "test",
    "org.scalatestplus" %% "mockito-5-12" % scalatestMockitoVersion % "test"
  )
}

// plugins
enablePlugins(BuildInfoPlugin, GitVersioning, GitBranchPrompt, sbtdocker.DockerPlugin, JavaAppPackaging)

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
docker / buildOptions := BuildOptions(
  //  cache = false,
  removeIntermediateContainers = BuildOptions.Remove.Always
  //  pullBaseImage = BuildOptions.Pull.Always
)

docker / imageNames := {
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
    case _ => latest
  }

  Seq(imageName)
}

docker / dockerfile := {
  val exposePort = 8080
  // The assembly task generates a fat JAR file
  val appDir: File = stage.value
  val targetDir = "/opt"

  new Dockerfile {
    from("hseeberger/scala-sbt:11.0.14.1_1.6.2_2.12.15")
    expose(exposePort)
    entryPoint(s"$targetDir/bin/${executableScriptName.value}")
    copy(appDir, targetDir)
  }
}

// releasing with the gitflow plugin
import sbtrelease.*
import allawala.sbt.sbtrelease.gitflow.Steps.*
import sbtrelease.ReleaseStateTransformations.*

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

Compile / packageBin  / mappings ~= { seq =>
  seq.filter{
    case (file, _) =>
      val fileName = file.getName
      !(fileName.equalsIgnoreCase("logback.xml") || fileName.startsWith("BuildInfo"))
  }
}

// Dependency Graph tree
//dependencyDotFile := file("dependencies.dot") //render dot file to `./dependencies.dot`

initialCommands :=
  """
import akka.actor._
import akka.pattern._
import akka.util._
import scala.concurrent._
import scala.concurrent.duration._
  """.stripMargin

resolvers += "Git Flow Snapshots" at "s3://s3-ap-southeast-2.amazonaws.com/maven.allawala.com/sbt-gitflow/snapshots"
resolvers += "Git Flow Releases" at "s3://s3-ap-southeast-2.amazonaws.com/maven.allawala.com/sbt-gitflow/releases"
resolvers += "Akka library repository".at("https://repo.akka.io/maven")

run / fork := true
// Running `sbt test` hangs without the following line, possibly due to https://github.com/sbt/sbt/issues/3022
Test / parallelExecution := false
