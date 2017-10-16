package allawala.chassis.auth.shiro.route

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.{Directive, Directive0, Directive1, Directives}
import allawala.chassis.auth.exception.{AuthenticationException, AuthorizationException}
import allawala.chassis.auth.shiro.model.{JWTAuthenticationToken, PrincipalType}
import allawala.chassis.auth.shiro.service.ShiroAuthService
import com.typesafe.scalalogging.StrictLogging
import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.subject.Subject

import scala.util.{Failure, Success, Try}

trait RouteSecurity extends Directives with StrictLogging {
  val AccessControlExposeHeader = "Access-Control-Expose-Headers"
  val Authorization = "Authorization"
  val RefreshToken = "Refresh-Token"
  val Bearer = "Bearer"
  type Directive2[T, U] = Directive[Tuple2[T, U]]

  def authService: ShiroAuthService

  def setAuthorizationHeader(token: String): Directive0 = {
    val accessControlAccessHeader = RawHeader(AccessControlExposeHeader, Authorization)
    val authorizationHeader = RawHeader(Authorization, s"$Bearer $token")
    respondWithHeaders(accessControlAccessHeader, authorizationHeader)
  }

  val authenticated: Directive1[Subject] = {
    (optionalHeaderValueByName(Authorization) & optionalHeaderValueByName(RefreshToken)).tflatMap {
      case (authToken, refreshToken) => authToken match {
        case None => throw AuthenticationException(message = "authorization header not found")
        case Some(auth) =>
          if (!auth.startsWith(Bearer)) {
            throw AuthenticationException(message = "bearer missing from authorization header")
          }
          else {
            val token = auth.split(' ').last
            val jwtSubject = authService.decodeToken(token, refreshToken)
            val subject = authService.authenticate(JWTAuthenticationToken(jwtSubject))
            if (jwtSubject.credentials != token) {
              // New token was issued due to the presence of a valid refresh token and needs to be sent back as response header
              setAuthorizationHeader(jwtSubject.credentials) & provide(subject)
            } else {
              provide(subject)
            }
          }
      }
    }
  }

  val onAuthenticated: Directive1[Subject] = {
    (optionalHeaderValueByName(Authorization) & optionalHeaderValueByName(RefreshToken)).tflatMap {
      case (authToken, refreshToken) => authToken match {
        case None => throw AuthenticationException(message = "authorization header not found")
        case Some(auth) =>
          if (!auth.startsWith(Bearer)) {
            throw AuthenticationException(message = "bearer missing from authorization header")
          }
          else {
            val token = auth.split(' ').last
            val jwtSubject = authService.decodeToken(token, refreshToken)
            onSuccess(authService.authenticateAsync(JWTAuthenticationToken(jwtSubject))).flatMap { subject =>
              if (jwtSubject.credentials != token) {
                // New token was issued due to the presence of a valid refresh token and needs to be sent back as response header
                setAuthorizationHeader(jwtSubject.credentials) & provide(subject)
              } else {
                provide(subject)
              }
            }
          }
      }
    }
  }

  def authorized(subject: Subject, permission: String): Directive0 = {
    if (!authService.isAuthorized(subject, permission)) throw AuthorizationException(message = "unauthorized")
    pass
  }

  def onAuthorized(subject: Subject, permission: String): Directive0 = {
    onSuccess(authService.isAuthorizedAsync(subject, permission)).flatMap { authorized =>
      if (!authorized) throw AuthorizationException(message = "unauthorized")
      pass
    }
  }

  def authorizedAny(subject: Subject, permissions: String*): Directive0 = {
    if (!authService.isAuthorizedAny(subject, permissions.toSet)) throw AuthorizationException(message = "unauthorized")
    pass
  }

  def onAuthorizedAny(subject: Subject, permissions: String*): Directive0 = {
    onSuccess(authService.isAuthorizedAnyAsync(subject, permissions.toSet)).flatMap { authorized =>
      if (!authorized) throw AuthorizationException(message = "unauthorized")
      pass
    }
  }

  def authorizedAll(subject: Subject, permissions: String*): Directive0 = {
    if (!authService.isAuthorizedAll(subject, permissions.toSet)) throw AuthorizationException(message = "unauthorized")
    pass
  }

  def onAuthorizedAll(subject: Subject, permissions: String*): Directive0 = {
    onSuccess(authService.isAuthorizedAllAsync(subject, permissions.toSet)).flatMap { authorized =>
      if (!authorized) throw AuthorizationException(message = "unauthorized")
      pass
    }
  }

  // TODO set refresh token
  def authenticate(user: String, password: String, rememberMe: Option[Boolean] = None): Directive2[Subject, String] = {
    val subject = authService.authenticateCredentials(new UsernamePasswordToken(user, password, rememberMe.getOrElse(false)))
    val primaryPrincipal = subject.getPrincipals.getPrimaryPrincipal.asInstanceOf[String]
    val token = authService.generateToken(PrincipalType.User, primaryPrincipal, rememberMe.getOrElse(false))
    setAuthorizationHeader(token) & tprovide((subject, token))
  }

  // TODO set refresh token
  def onAuthenticate(user: String, password: String, rememberMe: Option[Boolean] = None): Directive2[Subject, String] = {
    onSuccess(authService.authenticateCredentialsAsync(new UsernamePasswordToken(user, password, rememberMe.getOrElse(false)))).flatMap { subject =>
      val primaryPrincipal = subject.getPrincipals.getPrimaryPrincipal.asInstanceOf[String]
      val token = authService.generateToken(PrincipalType.User, primaryPrincipal, rememberMe.getOrElse(false))
      setAuthorizationHeader(token) & tprovide((subject, token))
    }
  }

  /*
    variant of authenticate that allows the caller to perform some action on failure for instance, update login attempt count.
   */
  // TODO set refresh token
  def authenticate(
                    user: String, password: String, rememberMe: Option[Boolean], onFailure: => Unit
                  ): Directive2[Subject, String] = {
    Try {
      val subject = authService.authenticateCredentials(new UsernamePasswordToken(user, password, rememberMe.getOrElse(false)))
      val primaryPrincipal = subject.getPrincipals.getPrimaryPrincipal.asInstanceOf[String]
      val token = authService.generateToken(PrincipalType.User, primaryPrincipal, rememberMe.getOrElse(false))
      setAuthorizationHeader(token) & tprovide((subject, token))
    } match {
      case Success(result) => result
      case Failure(e) =>
        onFailure
        throw e
    }
  }

  // TODO set refresh token
  def onAuthenticate(
                      user: String, password: String, rememberMe: Option[Boolean], onFailure: => Unit
                    ): Directive2[Subject, String] = {
    onComplete(authService.authenticateCredentialsAsync(new UsernamePasswordToken(user, password, rememberMe.getOrElse(false)))).flatMap {
      case Success(subject) =>
        val primaryPrincipal = subject.getPrincipals.getPrimaryPrincipal.asInstanceOf[String]
        val token = authService.generateToken(PrincipalType.User, primaryPrincipal, rememberMe.getOrElse(false))
        setAuthorizationHeader(token) & tprovide((subject, token))
      case Failure(e) =>
        onFailure
        throw e
    }
  }

  val jwtToken: Directive1[String] = headerValueByName("Authorization")
}
