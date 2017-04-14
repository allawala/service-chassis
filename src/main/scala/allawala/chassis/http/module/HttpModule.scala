package allawala.chassis.http.module

import javax.inject.Provider

import allawala.chassis.http.lifecycle.{LifecycleAware, LifecycleAwareRegistry}
import allawala.chassis.http.route.{HasRoute, PingRoute, RouteRegistry, Routes}
import allawala.chassis.util.{Registry, StringConverters}
import com.google.inject.matcher.Matchers
import com.google.inject.spi.{InjectionListener, TypeEncounter, TypeListener}
import com.google.inject.{AbstractModule, Provides, Singleton, TypeLiteral}
import net.codingwell.scalaguice.ScalaModule

import scala.reflect.{ClassTag,classTag}

class HttpModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    bind[PingRoute].asEagerSingleton()
    bind[Routes].asEagerSingleton()

    bindListener(Matchers.any(), new HasRouteTypeListener)
    bindListener(Matchers.any(), new LifecycleAwareTypeListener)
  }

  private abstract class HasTypeListener[T: ClassTag, U <: Registry[T]] extends TypeListener {
    val registryProvider: Provider[U]

    override def hear[I](typeLiteral: TypeLiteral[I], encounter: TypeEncounter[I]): Unit = {
      val clazz = typeLiteral.getRawType

      if (classTag[T].runtimeClass.isAssignableFrom(clazz)) encounter.register(new InjectionListener[I] {

        override def afterInjection(injectee: I): Unit = {
          val entry: T = injectee.asInstanceOf[T]
          val name = StringConverters.upperCamelToLowerHyphen.convert(entry.getClass.getSimpleName)
          registryProvider.get().register(name, entry)
        }
      })
    }
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