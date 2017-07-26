package allawala.chassis.health.model

case class HealthResult(buildDetails: BuildDetails, checks: Map[String, HealthCheckResult] = Map.empty[String, HealthCheckResult])
