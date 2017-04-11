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

    bindChecks()
    bind[HealthRoute].asEagerSingleton()
  }

  private def bindChecks() = {
    bindListener(Matchers.any(), new HealthCheckTypeListener)
  }

  private class HealthCheckTypeListener extends TypeListener {
    import com.google.common.base.{CaseFormat, Converter}

    private val healthCheckRegistryProvider = getProvider[HealthCheckRegistry]

    override def hear[I](typeLiteral: TypeLiteral[I], encounter: TypeEncounter[I]): Unit = {
      val clazz = typeLiteral.getRawType
      if (classOf[HealthCheck].isAssignableFrom(clazz)) encounter.register(new InjectionListener[I] {

        override def afterInjection(injectee: I): Unit = {
          val check: HealthCheck = injectee.asInstanceOf[HealthCheck]
          val name = converter.convert(check.getClass.getSimpleName)
          healthCheckRegistryProvider.get().register(name, check)
        }
      })
    }

    val converter: Converter[String, String] = CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_HYPHEN)
  }
}

object HealthModule {
  @Provides
  @Singleton
  def getHealthCheckRegistry(): HealthCheckRegistry = {
    new HealthCheckRegistry()
  }

}

