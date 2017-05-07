package allawala.chassis.health.route

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Route
import allawala.BuildInfo
import allawala.chassis.health.HealthCheckSupport
import allawala.chassis.health.model.{BuildDetails, HealthCheckResult, HealthResult}
import allawala.chassis.http.route.{HasRoute, RouteSupport}
import io.circe.generic.auto._

import scala.collection.JavaConverters._

/*
  NOTE: BuildInfo object is generated on compilation so standalone sbt clean will cause IDE to show errors
 */
class HealthRoute extends HasRoute with RouteSupport with HealthCheckSupport {

  override def route: Route = get {
    path("health") {
      checkHealth()
    } ~
      path("health" / "details") {
        checkHealth(true)
      }
  }

  private def checkHealth(detailed: Boolean = false) = {
    val result = registry.runHealthChecks.asScala.toMap.map { case (key, check) =>
      toCheckName(key) -> HealthCheckResult(check.isHealthy, check.getMessage, check.getTimestamp)
    }

    val healthy = result.values.toSeq.forall(_.isHealthy)

    val serviceStatus = if (!healthy) {
      ServiceUnavailable
    } else {
      OK
    }

    if (detailed) {
      val buildDetails = BuildDetails(BuildInfo.name, BuildInfo.version, BuildInfo.builtAtString, BuildInfo.gitCurrentBranch, BuildInfo.gitHeadCommit)
      complete(serviceStatus -> HealthResult(buildDetails, result))
    } else {
      complete(serviceStatus)
    }
  }
}
