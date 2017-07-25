package allawala.chassis.health.module

import allawala.chassis.health.checks.module.ChecksModule
import allawala.chassis.health.route.HealthRoute
import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule

class HealthModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    install(new ChecksModule)
    bind[HealthRoute].asEagerSingleton()
  }
}

