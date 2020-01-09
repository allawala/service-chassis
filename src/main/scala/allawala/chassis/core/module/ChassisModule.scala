  package allawala.chassis.core.module

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream.Materializer
import allawala.chassis.auth.module.AuthModule
import allawala.chassis.config.model.BaseConfig
import allawala.chassis.config.module.ConfigModule
import allawala.chassis.health.module.HealthModule
import allawala.chassis.http.module.HttpModule
import allawala.chassis.http.service.AkkaHttpService
import allawala.chassis.i18n.module.I18nModule
import allawala.chassis.util.module.UtilModule
import com.google.inject.{AbstractModule, Provides, Singleton}
import com.typesafe.config.Config
import javax.inject.Named
import net.codingwell.scalaguice.ScalaModule

import scala.concurrent.ExecutionContext

abstract class ChassisModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    bindConfigModule()
    bindHttpModule()
    bindHealthModule()
    bindUtilModule()
    bindI18nModule()
    bindAuthModule()

    bind[AkkaHttpService].asEagerSingleton()
  }

  protected def bindConfigModule(): Unit = {
    install(new ConfigModule)
  }

  protected def bindHttpModule(): Unit = {
    install(new HttpModule)
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

  @Provides
  @Singleton
  def getAkkaMaterializer(implicit actorSystem: ActorSystem): Materializer = {
    Materializer.matFromSystem
  }

  @Provides
  @Singleton
  def getActorSystem(baseConfig: BaseConfig, config: Config): ActorSystem = {
    ActorSystem(baseConfig.name, config)
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
