package allawala.chassis.auth.module

import allawala.chassis.auth.service._
import allawala.chassis.auth.shiro.module.ShiroAuthModule
import allawala.chassis.auth.shiro.service.{ShiroAuthService, ShiroAuthServiceImpl}
import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule

class AuthModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    bindAuthService()
    bindRefreshTokenService()
    bindJWTTokenService()
    bindTokenStorageService()
    configureShiroModule()
  }

  protected def bindAuthService(): Unit = {
    bind[ShiroAuthService].to[ShiroAuthServiceImpl].asEagerSingleton()
  }

  protected def bindRefreshTokenService(): Unit = {
    bind[RefreshTokenService].to[RefreshTokenServiceImpl].asEagerSingleton()
  }

  protected def bindJWTTokenService(): Unit = {
    bind[JWTTokenService].to[Rsa512JWTTokenServiceImpl].asEagerSingleton()
  }

  protected def bindTokenStorageService(): Unit = {
    bind[TokenStorageService].to[NoOpTokenStorageServiceImpl].asEagerSingleton()
  }

  protected def configureShiroModule(): Unit = {
    install(new ShiroAuthModule)
  }
}
