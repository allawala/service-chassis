package allawala.chassis.i18n.module

import allawala.chassis.i18n.service.I18nService
import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule

class I18nModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    bindI18nService()
  }

  protected def bindI18nService(): Unit = {
    bind[I18nService].asEagerSingleton()
  }
}
