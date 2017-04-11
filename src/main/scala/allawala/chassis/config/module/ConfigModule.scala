package allawala.chassis.config.module

import allawala.chassis.config.model.{Configuration, Environment}
import com.google.inject.{AbstractModule, Provider, Provides, Singleton}
import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus
import net.ceedubs.ficus.readers.ArbitraryTypeReader
import net.codingwell.scalaguice.ScalaModule

class ConfigModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {}

  def getConfigProvider: Provider[Configuration] = {
    getProvider[Configuration]
  }
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
  private val configuration = loadEnvConfiguration


  @Provides
  @Singleton
  def getConfig(): Configuration = {
    configuration
  }

  @Provides
  @Singleton
  def getEnvironment(): Environment = {
    environment
  }

  private def loadEnvConfiguration = {
    environment match {
      case Local => // Do nothing, load the default application.conf and logback.groovy
      case _ =>
        sys.props("logback.configurationFile") = s"logback.${environment.entryName}.groovy"
        sys.props("config.resource") = s"application.${environment.entryName}.conf"
    }
    ConfigFactory.invalidateCaches()
    ConfigFactory.load().as[Configuration]("service")
  }
}
