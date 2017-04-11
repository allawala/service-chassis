package allawala.chassis.health.route

import javax.inject.{Inject, Named}

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Route
import allawala.chassis.health.model.HealthCheckResult
import allawala.chassis.http.route.{HasRoute, RouteSupport}
import com.codahale.metrics.health.HealthCheckRegistry
import com.google.inject.Provider

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import io.circe.generic.auto._

/*
  Injecting the default dispatcher ec. If any of the checks need to do any blocking operations, this should be changed to the
  blocking dispatcher
 */
class HealthRoute @Inject()(val healthCheckRegistryProvider : Provider[HealthCheckRegistry])
                           (@Named("default-dispatcher") implicit val ec: ExecutionContext) extends HasRoute with RouteSupport {
  lazy val route: Route = get {
    path("health") {
      val checks = healthCheckRegistryProvider.get().runHealthChecks().values().asScala
      val unhealthy = checks.find(!_.isHealthy)
      val result = checks.map(check => HealthCheckResult(check.isHealthy, check.getMessage, check.getTimestamp))
      val serviceStatus = unhealthy.map(_ => ServiceUnavailable).getOrElse(OK)
      complete(serviceStatus -> result)
    }
  }
}
