package allawala.chassis.util.module

import allawala.chassis.util.{DataGenerator, DateTimeProvider}
import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule


class UtilModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    bind[DateTimeProvider].asEagerSingleton()
    bind[DataGenerator].asEagerSingleton()
  }
}