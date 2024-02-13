package allawala.chassis.health.checks

import allawala.chassis.health.HealthCheckSupport

import java.io.File

private[health] class LowDiskSpaceHealthCheck extends HealthCheckSupport {
  private val THRESHOLD = 10 * 1024 * 1024 // TODO specify in the config

  healthCheck(checkName) {
    import com.codahale.metrics.health.HealthCheck.Result
    val file = new File(".")
    val free = file.getFreeSpace

    if (free < THRESHOLD) {
      Result.unhealthy("Low disk space")
    }
    else {
      Result.healthy(s"$free bytes free")
    }
  }
}
