package allawala.chassis.http.module

import allawala.chassis.core.module.HasTypeListener
import allawala.chassis.http.lifecycle.{LifecycleAware, LifecycleAwareRegistry}
import allawala.chassis.http.route.{HasRoute, PingRoute, RouteRegistry, Routes}
import com.google.inject.matcher.Matchers
import com.google.inject.{AbstractModule, Provides, Singleton}
import javax.inject.Provider
import net.codingwell.scalaguice.ScalaModule

class HttpModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    bind[PingRoute].asEagerSingleton()
    bind[Routes].asEagerSingleton()

    bindListener(Matchers.any(), new HasRouteTypeListener)
    bindListener(Matchers.any(), new LifecycleAwareTypeListener)
  }

  private class HasRouteTypeListener extends HasTypeListener[HasRoute, RouteRegistry] {
    override val registryProvider: Provider[RouteRegistry] = getProvider[RouteRegistry]
  }

  private class LifecycleAwareTypeListener extends HasTypeListener[LifecycleAware, LifecycleAwareRegistry] {
    override val registryProvider: Provider[LifecycleAwareRegistry] = getProvider[LifecycleAwareRegistry]
  }
}

object HttpModule {
  @Provides
  @Singleton
  def getRouteRegistry(): RouteRegistry = {
    new RouteRegistry()
  }

  @Provides
  @Singleton
  def getLifecycleAwareRegistry(): LifecycleAwareRegistry = {
    new LifecycleAwareRegistry()
  }
}
