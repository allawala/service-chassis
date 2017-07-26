package allawala.chassis.auth.module

import allawala.chassis.auth.service.{InMemoryRefreshTokenService, RefreshTokenService}
import allawala.chassis.auth.shiro.module.ShiroAuthModule
import allawala.chassis.auth.shiro.service.{ShiroAuthService, ShiroAuthServiceImpl}
import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule

class AuthModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    bindAuthService()
    bindRefreshTokenService()
    configureShiroModule()
  }

  protected def bindAuthService(): Unit = {
    bind[ShiroAuthService].to[ShiroAuthServiceImpl].asEagerSingleton()
  }

  protected def bindRefreshTokenService(): Unit = {
    bind[RefreshTokenService].to[InMemoryRefreshTokenService].asEagerSingleton()
  }

  protected def configureShiroModule(): Unit = {
    install(new ShiroAuthModule)
  }
}
