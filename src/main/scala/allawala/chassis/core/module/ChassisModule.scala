  package allawala.chassis.core.module

import javax.inject.Named

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream.ActorMaterializer
import allawala.chassis.auth.module.AuthModule
import allawala.chassis.config.model.BaseConfig
import allawala.chassis.config.module.ConfigModule
import allawala.chassis.health.module.HealthModule
import allawala.chassis.http.module.HttpModule
import allawala.chassis.http.service.AkkaHttpService
import allawala.chassis.i18n.module.I18nModule
import allawala.chassis.util.module.UtilModule
import com.google.inject.{AbstractModule, Provides, Singleton}
import net.codingwell.scalaguice.ScalaModule

import scala.concurrent.ExecutionContext

abstract class ChassisModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    install(new ConfigModule)
    install(new HttpModule)
    bindHealthModule()
    bindUtilModule()
    bindI18nModule()
    bindAuthModule()

    bind[AkkaHttpService].asEagerSingleton()
  }

  protected def bindHealthModule(): Unit = {
    install(new HealthModule)
  }

  protected def bindUtilModule(): Unit = {
    install(new UtilModule)
  }

  protected def bindI18nModule(): Unit = {
    install(new I18nModule)
  }

  protected def bindAuthModule(): Unit = {
    install(new AuthModule)
  }
}

object ChassisModule {
  @Provides
  @Singleton
  def getAkkaMaterializer(implicit actorSystem: ActorSystem): ActorMaterializer = {
    ActorMaterializer()
  }

  @Provides
  @Singleton
  def getActorSystem(baseConfig: BaseConfig): ActorSystem = {
    ActorSystem(baseConfig.name)
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
