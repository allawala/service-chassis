package allawala.chassis.auth.shiro.route

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.{Directive0, Directive1, Directives}
import allawala.chassis.auth.exception.{AuthenticationException, AuthorizationException}
import allawala.chassis.auth.shiro.model.{JWTAuthenticationToken, JWTSubject, PrincipalType}
import allawala.chassis.auth.shiro.service.ShiroAuthService
import com.typesafe.scalalogging.StrictLogging
import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.subject.Subject

trait RouteSecurity extends Directives with StrictLogging {
  val Authorization = "Authorization"
  val RefreshToken = "Refresh-Token"
  val Bearer = "Bearer"

  def authService: ShiroAuthService

  def setAuthorizationHeader(token: String): Directive0 = {
    respondWithHeader(RawHeader(Authorization, s"$Bearer $token"))
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

  // TODO see if this can be converted to a Directive0
  def authorized(permission: String, subject: Subject): Directive1[Boolean] = {
    if (!authService.isAuthorized(permission, subject)) throw AuthorizationException(message = "unauthorized")
    provide(true)
  }

  def onAuthorized(permission: String, subject: Subject): Directive1[Boolean] = {
    onSuccess(authService.isAuthorizedAsync(permission, subject)).flatMap { authorized =>
      if (!authorized) throw AuthorizationException(message = "unauthorized")
      provide(true)
    }
  }

  // TODO set refresh token
  def authenticate(user: String, password: String, rememberMe: Option[Boolean] = None): Directive1[Subject] = {
    val subject = authService.authenticateCredentials(new UsernamePasswordToken(user, password, rememberMe.getOrElse(false)))
    val primaryPrincipal = subject.getPrincipals.getPrimaryPrincipal.asInstanceOf[String]
    val token = authService.generateToken(JWTSubject(PrincipalType.User, primaryPrincipal, password))
    setAuthorizationHeader(token) & provide(subject)
  }
}
