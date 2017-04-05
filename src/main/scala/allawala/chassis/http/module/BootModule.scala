  package allawala.chassis.http.module

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream.ActorMaterializer
import allawala.chassis.config.model.Configuration
import allawala.chassis.config.module.ConfigModule
import allawala.chassis.http.model.AkkaHttp
import com.google.inject.{AbstractModule, Module, Provides, Singleton}
import net.codingwell.scalaguice.ScalaModule

import scala.concurrent.ExecutionContext


class BootModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    install(new ConfigModule)

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
  def getDefaultExecutionContextExecutor(implicit actorSystem: ActorSystem) : ExecutionContext = {
    actorSystem.dispatcher
  }

  @Provides
  @Singleton
  def getLoggingAdapter(implicit actorSystem: ActorSystem) : LoggingAdapter = {
    Logging(actorSystem, getClass)
  }
}
