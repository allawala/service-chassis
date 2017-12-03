package allawala.chassis.auth.shiro.service

import javax.inject.{Inject, Named}

import allawala.ResponseFE
import allawala.chassis.auth.exception.AuthenticationException
import allawala.chassis.auth.model.{JWTSubject, PrincipalType, RefreshToken}
import allawala.chassis.auth.service.{JWTTokenService, RefreshTokenService, TokenStorageService}
import allawala.chassis.auth.shiro.model.{AuthenticatedSubject, JWTAuthenticationToken}
import allawala.chassis.config.model.{Auth, RefreshStrategy}
import allawala.chassis.core.exception.DomainException
import allawala.chassis.util.DateTimeProvider
import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.subject.Subject

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class ShiroAuthServiceImpl @Inject()(
                                      val auth: Auth,
                                      val dateTimeProvider: DateTimeProvider,
                                      val jwtTokenService: JWTTokenService,
                                      val refreshTokenService: RefreshTokenService,
                                      val tokenStorageService: TokenStorageService
                                    )
                                    (@Named("blocking-fixed-pool-dispatcher") implicit val ec: ExecutionContext)
  extends ShiroAuthService with ShiroSubjectSupport {

  override def authenticateCredentials(user: String, password: String, rememberMe: Boolean): ResponseFE[AuthenticatedSubject] =
    authenticateUserNamePassword(user, password, rememberMe)

  override def authenticateToken(jwtToken: String, refreshToken: Option[String]): ResponseFE[AuthenticatedSubject] =
    authenticateJwtToken(jwtToken, refreshToken)

  /*
    When impersonating a subject, we only care about getting an authenticated subject, we dont care about reissuing tokens
   */
  override def impersonateSubjectSync(principal: String, credentials: String): Either[DomainException, Subject] = {
    authenticateJwtSubject(JWTSubject(PrincipalType.ImpersonatedUser, principal, credentials)) { subject =>
      subject
    }
  }

  override def impersonateSubject(principal: String, credentials: String): ResponseFE[Subject] = Future {
    impersonateSubjectSync(principal, credentials)
  }

  override def isAuthorizedSync(subject: Subject, permission: String): Boolean = {
    subject.isPermitted(permission)
  }

  override def isAuthorized(subject: Subject, permission: String): Future[Boolean] = Future {
    isAuthorizedSync(subject, permission)
  }

  override def isAuthorizedAnySync(subject: Subject, permissions: Set[String]): Boolean = {
    permissions.find(isAuthorizedSync(subject, _)) match {
      case Some(_) => true
      case None => false
    }
  }

  override def isAuthorizedAny(subject: Subject, permissions: Set[String]): Future[Boolean] = {
    val fSeq = Future.sequence(permissions.map(isAuthorized(subject, _)))
    fSeq map { auths =>
      auths.contains(true)
    }
  }

  override def isAuthorizedAllSync(subject: Subject, permissions: Set[String]): Boolean = {
    permissions.forall(isAuthorizedSync(subject, _))
  }

  override def isAuthorizedAll(subject: Subject, permissions: Set[String]): Future[Boolean] = {
    val fSeq = Future.sequence(permissions.map(isAuthorized(subject, _)))
    fSeq map { auths =>
      auths.forall(_ == true)
    }
  }

  override def invalidate(jwtToken: String, refreshToken: Option[String]): ResponseFE[Unit] = {
    // Since we are removing the token, we dont care if its expired
    jwtTokenService.decodeExpiredToken(jwtToken) match {
      case Left(e) =>
        Future.successful(Left(AuthenticationException(cause = e)))
      case Right(jwtSubject) =>
        val selectorToRemove = refreshToken match {
          case Some(encodedRefreshToken) =>
            val decoded = refreshTokenService.decodeSelectorAndToken(encodedRefreshToken)
            decoded match {
              case Some((selector, _)) => Some(selector)
              case None => None
            }
          case None => None
        }
        tokenStorageService.removeTokens(jwtSubject.principalType, jwtSubject.principal, jwtToken, selectorToRemove)
    }
  }

  override def invalidateAll(jwtToken: String): ResponseFE[Unit] = {
    // Since we are removing the token, we dont care if its expired
    jwtTokenService.decodeExpiredToken(jwtToken) match {
      case Left(e) =>
        Future.successful(Left(AuthenticationException(cause = e)))
      case Right(jwtSubject) =>
        tokenStorageService.removeAllTokens(jwtSubject.principalType, jwtSubject.principal)
    }
  }

  // Authenticate via the UsernamePasswordTokenRealm
  private def authenticateUserNamePassword(user: String, password: String, rememberMe: Boolean) = {
    val jwtToken = jwtTokenService.generateToken(PrincipalType.User, user, rememberMe)

    val refreshToken = if (rememberMe && RefreshStrategy.isFull(auth.expiration.refreshTokenStrategy)) {
      Some(refreshTokenService.generateToken())
    } else {
      None
    }

    Try {
      val subject = buildSubject
      subject.login(new UsernamePasswordToken(user, password, rememberMe))
      AuthenticatedSubject(subject, jwtToken, refreshToken.flatMap(_.encodedSelectorAndToken))
    } match {
      case Success(authenticatedSubject) =>
        // If the authentication is successful, store the new tokens
        tokenStorageService.storeTokens(PrincipalType.User, user, jwtToken, refreshToken) map {
          case Left(e) => Left(AuthenticationException(cause = e))
          case Right(_) => Right(authenticatedSubject)
        }
      case Failure(e) => Future.successful(Left(AuthenticationException(cause = e)))
    }
  }

  // Authenticate via the JwtRealm
  private def authenticateJwtToken(jwtToken: String, refreshToken: Option[String]) = {
    jwtTokenService.decodeToken(jwtToken) match {
      case Left(e) =>
        // Possibly expired
        jwtTokenService.decodeExpiredToken(jwtToken) match {
          case Left(ex) => Future.successful(Left(AuthenticationException(cause = ex)))
          case Right(expiredJwtSubject) if refreshToken.isDefined =>
            // Expired token but we have a reissue token
            reissueTokensAndAuthenticate(expiredJwtSubject, refreshToken.get)
          case Right(expiredJwtSubject) =>
            removeExpiredTokens(expiredJwtSubject, None, "jwt token expired")
        }
      case Right(jwtSubject) =>
        // token still valid and no refresh token
        Future.successful(authenticateJwtSubject(jwtSubject) { subject =>
          AuthenticatedSubject(subject, jwtToken, None)
        })
    }
  }

  private def reissueTokensAndAuthenticate(expiredJwtSubject: JWTSubject, encodedRefreshToken: String) = {
    val decoded = refreshTokenService.decodeSelectorAndToken(encodedRefreshToken)
    decoded match {
      case Some((selector, validator)) =>
        tokenStorageService.lookupTokens(selector) flatMap {
          case Left(e) => removeExpiredTokens(expiredJwtSubject, None, "refresh token not found", Some(e))
          case Right((existingJwtToken, existingRefreshToken)) =>
            if (existingJwtToken != expiredJwtSubject.credentials) {
              // Refresh token has expired
              removeExpiredTokens(expiredJwtSubject, Some(existingRefreshToken), "jwt token mismatch")
            }
            else if (dateTimeProvider.now.isAfter(existingRefreshToken.expires)) {
              // Refresh token has expired
              removeExpiredTokens(expiredJwtSubject, Some(existingRefreshToken), "refresh token expired")
            } else {
              val hashed = refreshTokenService.hashToken(validator)
              if (constantTimeEquals(hashed, existingRefreshToken.tokenHash)) {
                reissueAndAuthenticate(expiredJwtSubject, existingRefreshToken)
              } else {
                removeExpiredTokens(expiredJwtSubject, Some(existingRefreshToken), "token hash mismatch")
              }
            }
        }
      case None => removeExpiredTokens(expiredJwtSubject, None, "invalid refresh token")
    }
  }

  private def reissueAndAuthenticate(expiredJwtSubject: JWTSubject, existingRefreshToken: RefreshToken) = {
    val refreshToken = refreshTokenService.generateToken()
    // If we get here, remember me must be true
    val jwtToken = jwtTokenService.generateToken(expiredJwtSubject.principalType, expiredJwtSubject.principal, rememberMe = true)
    /*
      Since token expiration, decoding and refreshing is handled here, shiro realm should not check those again.
      It should validate for instance, if the user is still active, or if the jwt token is in a list of tokens maintained for
      the user.
      IMPORTANT!!! At this stage, it checks the expired token in the list as we have not rotated the tokens out at this point.
      tokens are rotated if the authentication succeeds.
      This means that for the call in which the token is being refreshed, the realm auth sees the old creds. The next call will
      see the new creds
     */
    authenticateJwtSubject(expiredJwtSubject) { subject =>
      AuthenticatedSubject(subject, jwtToken, refreshToken.encodedSelectorAndToken)
    } match {
      case Left(e) => Future.successful(Left(e))
      case Right(authenticatedSubject) =>
        // New token and refresh token
        // Successfully authenticated, rotate the token
        tokenStorageService.rotateTokens(
          expiredJwtSubject.principalType, expiredJwtSubject.principal,
          expiredJwtSubject.credentials, jwtToken,
          existingRefreshToken, refreshToken
        ) map {
          case Left(e) => Left(
            AuthenticationException(logMap = Map("reason" -> "token rotation failed"), cause = e))
          case Right(_) => Right(authenticatedSubject)
        }
    }
  }

  private def authenticateJwtSubject[T](jwtSubject: JWTSubject)(f: Subject => T) = {
    Try {
      val subject = buildSubject
      subject.login(JWTAuthenticationToken(jwtSubject))
      subject
    } match {
      case Success(subject) => Right(f(subject))
      case Failure(e) => Left(AuthenticationException(cause = e))
    }
  }

  private def removeExpiredTokens(
                                   expiredJwtSubject: JWTSubject,
                                   expiredRefreshToken: Option[RefreshToken],
                                   reason: String,
                                   cause: Option[Throwable] = None
                                 ) = {
    val principalType = expiredJwtSubject.principalType
    val principal = expiredJwtSubject.principal
    val expiredJwtToken = expiredJwtSubject.credentials

    tokenStorageService.removeTokens(principalType, principal, expiredJwtToken, expiredRefreshToken.map(_.selector)) map {
      case Left(e) =>
        Left(AuthenticationException(cause = e, logMap = Map("reason" -> "expired token removal failed")))
      case Right(_) =>
        cause match {
          case Some(e) =>
            Left(AuthenticationException(cause = e, logMap = Map("reason" -> reason)))
          case None =>
            Left(AuthenticationException(logMap = Map("reason" -> reason)))
        }
    }
  }

  // From com.softwaremill.session.SessionUtil
  //
  // Do not change this unless you understand the security issues behind timing attacks.
  // This method intentionally runs in constant time if the two strings have the same length.
  // If it didn't, it would be vulnerable to a timing attack.
  private def constantTimeEquals(a: String, b: String) = {
    if (a.length != b.length) {
      false
    }
    else {
      var equal = 0
      for (i <- Array.range(0, a.length)) {
        equal |= a(i) ^ b(i)
      }
      equal == 0
    }
  }
}
