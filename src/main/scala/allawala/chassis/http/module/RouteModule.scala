package allawala.chassis.http.module

import allawala.chassis.http.model.RouteRegistry
import allawala.chassis.http.route.{HasRoute, Routes, PingRoute}
import com.google.inject.matcher.Matchers
import com.google.inject.spi.{InjectionListener, TypeEncounter, TypeListener}
import com.google.inject.{AbstractModule, Provides, Singleton, TypeLiteral}
import net.codingwell.scalaguice.ScalaModule

class RouteModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    bind[PingRoute].asEagerSingleton()
    bind[Routes].asEagerSingleton()

    bindListener(Matchers.any(), new HasRouteTypeListener)
  }


  private class HasRouteTypeListener extends TypeListener {
    import com.google.common.base.{CaseFormat, Converter}

    private val routeRegistryProvider = getProvider[RouteRegistry]

    override def hear[I](typeLiteral: TypeLiteral[I], encounter: TypeEncounter[I]): Unit = {
      val clazz = typeLiteral.getRawType
      if (classOf[HasRoute].isAssignableFrom(clazz)) encounter.register(new InjectionListener[I] {

        override def afterInjection(injectee: I): Unit = {
          val check: HasRoute = injectee.asInstanceOf[HasRoute]
          val name = converter.convert(check.getClass.getSimpleName)
          routeRegistryProvider.get().register(name, check)
        }
      })
    }

    val converter: Converter[String, String] = CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_HYPHEN)
  }
}

object RouteModule {
  @Provides
  @Singleton
  def getRouteRegistry(): RouteRegistry = {
    new RouteRegistry()
  }

}