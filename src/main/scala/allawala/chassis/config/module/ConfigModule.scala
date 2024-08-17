package allawala.chassis.config.module

import allawala.chassis.config.model._
import com.google.inject.{AbstractModule, Provides, Singleton}
import com.typesafe.config.{Config, ConfigFactory}
import net.ceedubs.ficus.Ficus
import net.ceedubs.ficus.readers.ArbitraryTypeReader
import net.codingwell.scalaguice.ScalaModule
import ArbitraryTypeReader._
import Environment._
import Ficus._

class ConfigModule extends AbstractModule with ScalaModule {
  /*
   This is created here instead of within the getConfig provider is because we do not have access to the provider in the logback
   groovy configuration and calling getconfig directly would have loaded the configuration again.
  */
  private val environment = loadEnvironment()
  private val config = loadEnvConfig

  override def configure(): Unit = {}

  @Provides
  @Singleton
  def getBaseConfig(): BaseConfig = {
    config.as[BaseConfig]("service.baseConfig")
  }

  @Provides
  @Singleton
  def getConfig(): Config = {
    config
  }

  @Provides
  @Singleton
  def getEnvironment(): Environment = {
    environment
  }

  @Provides
  @Singleton
  def getAuth(config: BaseConfig): Auth = {
    config.auth
  }

  @Provides
  @Singleton
  def getCorsConfig(config: BaseConfig): CorsConfig = {
    config.corsConfig
  }

  @Provides
  @Singleton
  def getLanguageConfig(config: BaseConfig): LanguageConfig = {
    config.languageConfig
  }

  private def loadEnvConfig = {
    environment match {
      case Local => // Do nothing, load the default logback.xml and application.conf
      case _ =>
        sys.props("logback.configurationFile") = s"logback.${environment.entryName}.xml"
        sys.props("config.resource") = s"application.${environment.entryName}.conf"
    }
    ConfigFactory.invalidateCaches()
    loadConfig(environment)
  }

  protected def loadConfig(environment: Environment): Config = {
    ConfigFactory.load()
  }

  protected def loadEnvironment(): Environment = {
    sys.env.get("ENV").flatMap(Environment.withNameInsensitiveOption).getOrElse(Local)
  }
}
