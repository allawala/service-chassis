package allawala.chassis.health.checks.module

import allawala.chassis.health.checks.LowDiskSpaceHealthCheck
import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule

private[health] class ChecksModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    bind[LowDiskSpaceHealthCheck].asEagerSingleton()
  }
}
