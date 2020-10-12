import sbt.Keys._
import sbt._
import sbtbuildinfo.BuildInfoKeys.{buildInfoKeys, buildInfoOptions, buildInfoPackage}
import sbtbuildinfo.{BuildInfoKey, BuildInfoOption}
import scala.sys.process._

name := """service-chassis"""

organization := "allawala"

scalaVersion := "2.12.10"

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
  val akkaVersion = "2.6.10"
  val akkaHttpCorsVersion = "1.1.0"
  val akkaHttpVersion = "10.2.1"
  val akkaHttpCirceVersion = "1.35.0"
  val beanUtilsVersion = "1.9.4"
  val bouncyCastleVersion = "1.64"
  val circeVersion = "0.12.3"
  val circeOpticsVersion = "0.12.0"
  val circeGenericsVersion = "0.12.2"
  val enumeratumVersion = "1.5.13"
  val enumeratumCirceVersion = "1.5.22"
  val ficusVersion = "1.4.7"
  val groovyVersion = "2.4.10"
  val guiceVersion = "4.1.0"
  val scalaGuiceVersion = "4.1.0"
  val jwtCirceVersion = "4.1.0"
  val logbackVersion = "1.2.3"
  val metricsVersion = "3.5.6_a2.4"
  val mockitoVersion = "2.8.47"
  val scalaI18nVersion = "1.0.2"
  val scalaLoggingVersion = "3.5.0"
  val scalatestVersion = "3.0.1"
  val slf4jVersion = "1.7.25"
  val shiroVersion = "1.4.0"
  val threeTenExtraVersion = "1.2"

  Seq(
    // Akka
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "ch.megard" %% "akka-http-cors" % akkaHttpCorsVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,

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
    "com.pauldijou" %% "jwt-circe" % jwtCirceVersion,

    // Logging
    "org.codehaus.groovy" % "groovy-all" % groovyVersion, // To allow log config to be defined in groovy
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "ch.qos.logback" % "logback-classic" % logbackVersion,
    "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion,
    "org.slf4j" % "jcl-over-slf4j" % slf4jVersion,
    "org.slf4j" % "log4j-over-slf4j" % slf4jVersion,

    // Metrics
    "nl.grons" %% "metrics-scala" % metricsVersion,

    // Shiro
    "org.apache.shiro" % "shiro-core" % shiroVersion,
    "org.apache.shiro" % "shiro-guice" % shiroVersion,

    // Threeten Extras
    "org.threeten" % "threeten-extra" % threeTenExtraVersion,

    // i18n
    "com.osinka.i18n" %% "scala-i18n" % scalaI18nVersion,

    // Test Dependencies
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test",
    "org.mockito" % "mockito-core" % mockitoVersion % "test",
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

  new Dockerfile {
    from("hseeberger/scala-sbt:8u141-jdk_2.12.3_0.13.16")
    expose(exposePort)
    copy(artifact, artifactTargetPath)
    entryPoint("java", "-jar", artifactTargetPath)
  }
}

// releasing with the gitflow plugin
import sbtrelease._
import allawala.sbt.sbtrelease.gitflow.Steps._
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

fork in run := true
// Running `sbt test` hangs without the following line, possibly due to https://github.com/sbt/sbt/issues/3022
parallelExecution in Test := false
