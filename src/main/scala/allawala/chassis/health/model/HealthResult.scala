package allawala.chassis.health.model

case class HealthResult(buildDetails: BuildDetails, checks: Seq[HealthCheckResult] = Seq.empty[HealthCheckResult])
