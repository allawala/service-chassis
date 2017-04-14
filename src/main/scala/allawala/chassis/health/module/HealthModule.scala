package allawala.chassis.health.module

import allawala.chassis.health.checks.module.ChecksModule
import allawala.chassis.health.route.HealthRoute
import com.codahale.metrics.health.{HealthCheck, HealthCheckRegistry}
import com.google.inject.matcher.Matchers
import com.google.inject.spi.{InjectionListener, TypeEncounter, TypeListener}
import com.google.inject.{AbstractModule, Provides, Singleton, TypeLiteral}
import net.codingwell.scalaguice.ScalaModule

class HealthModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    install(new ChecksModule)
    bind[HealthRoute].asEagerSingleton()
  }
}

