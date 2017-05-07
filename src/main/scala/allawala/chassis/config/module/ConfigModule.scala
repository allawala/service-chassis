package allawala.chassis.config.module

import allawala.chassis.config.model.{Auth, BaseConfig, Environment}
import com.google.inject.{AbstractModule, Provides, Singleton}
import com.typesafe.config.{Config, ConfigFactory}
import net.ceedubs.ficus.Ficus
import net.ceedubs.ficus.readers.ArbitraryTypeReader
import net.codingwell.scalaguice.ScalaModule

class ConfigModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {}
}

object ConfigModule {

  import ArbitraryTypeReader._
  import Environment._
  import Ficus._

  /*
   This is created here instead of within the getConfig provider is because we do not have access to the provider in the logback
   groovy configuration and calling getconfig directly would have loaded the configuration again.
  */
  private val environment = sys.env.get("ENV").flatMap(Environment.withNameInsensitiveOption).getOrElse(Local)
  private val config = loadEnvConfig


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

  private def loadEnvConfig = {
    environment match {
      case Local => // Do nothing, load the default application.conf and logback.groovy
      case _ =>
        sys.props("logback.configurationFile") = s"logback.${environment.entryName}.groovy"
        sys.props("config.resource") = s"application.${environment.entryName}.conf"
    }
    ConfigFactory.invalidateCaches()
    ConfigFactory.load()
  }
}
