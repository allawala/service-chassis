package allawala.chassis.auth.shiro.service

import java.time.{Duration, Instant}
import java.time.temporal.TemporalAmount
import javax.inject.{Inject, Named}

import allawala.chassis.auth.service.RefreshTokenService
import allawala.chassis.auth.shiro.model.{JWTAuthenticationToken, JWTSubject, PrincipalType}
import allawala.chassis.config.model.Auth
import allawala.chassis.core.exception.ServerException
import io.circe.Json
import io.circe.parser._
import org.apache.shiro.authc.{AuthenticationException, AuthenticationToken, UsernamePasswordToken}
import org.apache.shiro.subject.Subject
import pdi.jwt.exceptions.JwtExpirationException
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtOptions}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ShiroAuthServiceImpl @Inject()(val auth: Auth, val refreshTokenService: RefreshTokenService)
                                    (@Named("blocking-fixed-pool-dispatcher") implicit val ec: ExecutionContext)
  extends ShiroAuthService {

  override def generateToken(
                              principalType: PrincipalType,
                              principal: String,
                              expiresIn: TemporalAmount
                            ): String = {
    val Right(claimJson) = parse(
      s"""
         |{
         |"iat":${Instant.now.getEpochSecond},
         |"exp":${Instant.now.plus(expiresIn).getEpochSecond},
         |"sub":"$principal",
         |"typ":"${principalType.entryName}",
         |"rnd":${Instant.now.toEpochMilli}
         |}
         |""".stripMargin
    )

    JwtCirce.encode(claimJson, auth.rsa.privateKey, JwtAlgorithm.RS512)
  }

  override def decodeToken(token: String, refreshToken: Option[String]): JWTSubject = {
    import io.circe.optics.JsonPath._

    def getSubjectDetails(json: Json): (String, String) = {
      val typ = root.typ.string.getOption(json)
        .getOrElse(throw new AuthenticationException("JWT token invalid. Missing typ"))
      val sub = root.sub.string.getOption(json)
        .getOrElse(throw new AuthenticationException("JWT token invalid. Missing subject"))
      (typ, sub)
    }

    JwtCirce.decodeJson(token, auth.rsa.publicKey, Seq(JwtAlgorithm.RS512)) match {
      case Success(json) =>
        val (typ, sub) = getSubjectDetails(json)
        JWTSubject(PrincipalType.withName(typ), sub, token)
      case Failure(e) =>
        e match {
          case _: JwtExpirationException if refreshToken.isDefined =>
            decodeSelectorAndToken(refreshToken.get) match {
              case Some((selector, tok)) =>
                refreshTokenService.lookup(selector) match {
                  case Some(result) =>
                    // TODO validate the refresh token hash and expiration
                    val Right(json) =
                      JwtCirce.decodeJson(token, auth.rsa.publicKey, Seq(JwtAlgorithm.RS512), JwtOptions(expiration = false))
                        .toEither
                    val (typ, sub) = getSubjectDetails(json)
                    val subject = JWTSubject(PrincipalType.withName(typ), sub, token)
                    val newToken = generateToken(subject.principalType, subject.principal)
                    subject.copy(credentials = newToken)
                  case None => throw new AuthenticationException("Refresh token not found", e)
                }

              case None => throw new AuthenticationException("Failed to decode refresh token")
            }
          case _ => throw new AuthenticationException("JWT authentication failed", e)

        }
    }
  }

  override def authenticate(authToken: JWTAuthenticationToken): Subject = {
    authenticateToken(authToken)
  }

  override def authenticateAsync(authToken: JWTAuthenticationToken): Future[Subject] = Future {
    authenticateToken(authToken)
  }

  override def isAuthorized(permission: String, subject: Subject): Boolean = {
    subject.isPermitted(permission)
  }

  override def isAuthorizedAsync(permission: String, subject: Subject): Future[Boolean] = Future {
    isAuthorized(permission, subject)
  }

  override def authenticateCredentials(authToken: UsernamePasswordToken): Subject = {
    authenticateToken(authToken)
  }

  override def authenticateCredentialsAsync(authToken: UsernamePasswordToken): Future[Subject] = Future {
    authenticateToken(authToken)
  }

  private def authenticateToken(authToken: AuthenticationToken) = {
    val subject = (new Subject.Builder).buildSubject
    /*
      This guard is purely for sanity in case there is some shiro internal logic that I have missed that still depends on
      thread local context. To be removed after some load testing that will cause the same dispatcher thread to be used again.
     */
    if (subject.isAuthenticated) {
      throw ServerException(message = "subject pre-authenticated, possible thread context issue")
    }
    subject.login(authToken)
    subject
  }

  /*
    From com.softwaremill.session.SessionManager
   */
  private def decodeSelectorAndToken(value: String): Option[(String, String)] = {
    val s = value.split(":", 2)
    if (s.length == 2) Some((s(0), s(1))) else None
  }
}
