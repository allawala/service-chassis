package allawala.chassis.health.checks

import java.io.File

import com.codahale.metrics.health.HealthCheck

private[health] class LowDiskSpaceHealthCheck extends HealthCheck {
  private val THRESHOLD = 10 * 1024 * 1024 // TODO specify in the config

  override def check(): HealthCheck.Result = {
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
