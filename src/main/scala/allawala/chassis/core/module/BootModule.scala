  package allawala.chassis.core.module

import javax.inject.Named

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream.ActorMaterializer
import allawala.chassis.config.model.Configuration
import allawala.chassis.config.module.ConfigModule
import allawala.chassis.http.model.AkkaHttp
import allawala.chassis.http.module.RouteModule
import com.google.inject.{AbstractModule, Module, Provides, Singleton}
import net.codingwell.scalaguice.ScalaModule

import scala.concurrent.ExecutionContext


class BootModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    install(new ConfigModule)
    install(new RouteModule)

    bind[AkkaHttp].asEagerSingleton()
  }
}

object BootModule {
  def apply(): Module = new BootModule

  @Provides
  @Singleton
  def getAkkaMaterializer(implicit actorSystem: ActorSystem): ActorMaterializer = {
    ActorMaterializer()
  }

  @Provides
  @Singleton
  def getActorSystem(config: Configuration): ActorSystem = {
    ActorSystem(config.name)
  }

  @Provides
  @Singleton
  @Named("default-dispatcher")
  def getDefaultExecutionContextExecutor(implicit actorSystem: ActorSystem) : ExecutionContext = {
    actorSystem.dispatcher
  }

  @Provides
  @Singleton
  @Named("blocking-fixed-pool-dispatcher")
  def getBlockingExecutionContextExecutor(implicit actorSystem: ActorSystem) : ExecutionContext = {
    actorSystem.dispatchers.lookup("blocking-fixed-pool-dispatcher")
  }

  @Provides
  @Singleton
  def getLoggingAdapter(implicit actorSystem: ActorSystem) : LoggingAdapter = {
    Logging(actorSystem, getClass)
  }
}
