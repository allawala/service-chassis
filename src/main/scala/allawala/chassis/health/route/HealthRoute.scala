package allawala.chassis.health.route

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Route
import allawala.BuildInfo
import allawala.chassis.health.HealthCheckSupport
import allawala.chassis.health.model.{BuildDetails, HealthCheckResult, HealthResult}
import allawala.chassis.http.route.{HasRoute, RouteSupport}
import allawala.chassis.i18n.service.I18nService
import io.circe.generic.auto._

import javax.inject.Inject
import scala.jdk.CollectionConverters._

/*
  NOTE: BuildInfo object is generated on compilation so standalone sbt clean will cause IDE to show errors
 */
class HealthRoute @Inject()(override val i18nService: I18nService) extends HasRoute with RouteSupport with HealthCheckSupport {

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
      val buildDetails = BuildDetails(
        getOrNone(BuildInfo.name),
        getOrNone(BuildInfo.version),
        getOrNone(BuildInfo.builtAtString),
        getOrNone(BuildInfo.gitCurrentBranch),
        getOrNone(BuildInfo.gitHeadCommit).flatten
      )
      complete(serviceStatus -> HealthResult(buildDetails, result))
    } else {
      complete(serviceStatus)
    }
  }

  private def getOrNone[T](evalulate: => T) = {
    try {
      Some(evalulate)
    } catch {
      case t: Throwable => None
    }
  }
}
