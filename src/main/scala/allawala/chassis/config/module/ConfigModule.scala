package allawala.chassis.config.module

import allawala.chassis.config.model.Configuration
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
  import Ficus._

  @Provides
  @Singleton
  def getConfig(): Configuration = {
    val config: Config = ConfigFactory.load()
    config.as[Configuration]("service")
  }
}
