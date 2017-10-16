package allawala.chassis.auth.shiro.service

import java.time.temporal.TemporalAmount
import java.time.{Instant, Duration => JDuration}
import javax.inject.{Inject, Named}

import allawala.chassis.auth.service.RefreshTokenService
import allawala.chassis.auth.shiro.model.{JWTAuthenticationToken, JWTSubject, PrincipalType}
import allawala.chassis.config.model.{Auth, RefreshStrategy}
import allawala.chassis.core.exception.ServerException
import io.circe.Json
import io.circe.parser._
import org.apache.shiro.authc.{AuthenticationException, AuthenticationToken, UsernamePasswordToken}
import org.apache.shiro.subject.Subject
import org.threeten.extra.Temporals
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

  // TODO refactor this so that it generates both the jwt token and the optional refresh token
  override def generateToken(
                              principalType: PrincipalType,
                              principal: String,
                              rememberMe: Boolean
                            ): String = {
    def expiresIn = {
      val expiration = auth.expiration
      val duration = if (!rememberMe) {
        expiration.expiry
      } else {
        RefreshStrategy.withName(expiration.refreshTokenStrategy) match {
          case RefreshStrategy.Full => expiration.expiry
          case RefreshStrategy.Simple => expiration.refreshTokenExpiry // issue the token using the refreshTokenExpiry
        }
      }

      JDuration.of(duration._1, Temporals.chronoUnit(duration._2))
    }

    generateToken(principalType, principal, expiresIn)
  }

  /*
    Important!! This will fail to decode if its expired. If we need to check if an expired token can be decoded, we will need
    another method
   */
  override def canDecodeToken(token: String): Boolean = {
    JwtCirce.decodeJson(token, auth.rsa.publicKey, Seq(JwtAlgorithm.RS512)) match {
      case Success(_) => true
      case Failure(_) => false
    }
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
                    /*
                      If we are here, the token has expired and the refresh token exists, so remember me must be true
                     */
                    val newToken = generateToken(subject.principalType, subject.principal, rememberMe = true)
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

  override def isAuthorized(subject: Subject, permission: String): Boolean = {
    subject.isPermitted(permission)
  }

  override def isAuthorizedAsync(subject: Subject, permission: String): Future[Boolean] = Future {
    isAuthorized(subject, permission)
  }

  override def isAuthorizedAny(subject: Subject, permissions: Set[String]): Boolean = {
    permissions.find(isAuthorized(subject, _)) match {
      case Some(_) => true
      case None => false
    }
  }

  override def isAuthorizedAnyAsync(subject: Subject, permissions: Set[String]): Future[Boolean] = {
    val fSeq = Future.sequence(permissions.map(isAuthorizedAsync(subject, _)))
    fSeq map { auths =>
      auths.contains(true)
    }
  }

  override def isAuthorizedAll(subject: Subject, permissions: Set[String]): Boolean = {
    permissions.forall(isAuthorized(subject, _))
  }

  override def isAuthorizedAllAsync(subject: Subject, permissions: Set[String]): Future[Boolean] = {
    val fSeq = Future.sequence(permissions.map(isAuthorizedAsync(subject, _)))
    fSeq map { auths =>
      auths.forall(_ == true)
    }
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
