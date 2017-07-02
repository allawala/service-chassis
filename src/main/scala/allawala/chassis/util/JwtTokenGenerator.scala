package allawala.chassis.util

import java.time.Duration

import allawala.chassis.auth.model.{RefreshToken, RefreshTokenLookupResult}
import allawala.chassis.auth.service.RefreshTokenService
import allawala.chassis.auth.shiro.model.PrincipalType
import allawala.chassis.auth.shiro.service.ShiroAuthServiceImpl
import allawala.chassis.config.model.{Auth, Environment}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging
import net.ceedubs.ficus.Ficus
import net.ceedubs.ficus.readers.ArbitraryTypeReader

import scala.concurrent.ExecutionContext

/*
  Utility class to generate jwt token for all environments. Only should be used to generate service tokens.
  To run, call eg. JwtTokenGenerator.generate("service-name", 100)
  Update the service name to the name of the service for which the token is intended.
 */
object JwtTokenGenerator extends StrictLogging {
  def generate(environment: Environment, service: String, expiresInDays: Int): Unit = {
    import ArbitraryTypeReader._
    import Ficus._

    val file =  environment match {
      case Environment.Local => "application.conf"
      case _ => s"application.${environment.entryName}.conf"
    }

    val config = ConfigFactory.load(file)
    val auth = config.as[Auth]("service.baseConfig.auth")
    val authService = new ShiroAuthServiceImpl(auth, new NoOpRefreshTokenService)(ExecutionContext.global)
    val token = authService.generateToken(PrincipalType.Service, service, Duration.ofDays(expiresInDays.toLong))
    logger.debug(s"${environment.entryName} : $token")
  }

  private class NoOpRefreshTokenService extends RefreshTokenService {
    override def lookup(selector: String): Option[RefreshTokenLookupResult] = None

    override def store(refreshToken: RefreshToken): Unit = ()

    override def remove(selector: String): Unit = ()
  }
}
