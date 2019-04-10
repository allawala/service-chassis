package allawala.chassis.auth.shiro.route

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.{Directive0, Directive1, Directives}
import akka.stream.ActorMaterializer
import allawala.ResponseFE
import allawala.chassis.auth.exception.{AuthenticationException, AuthorizationException}
import allawala.chassis.auth.shiro.model.AuthenticatedSubject
import allawala.chassis.auth.shiro.service.ShiroAuthService
import allawala.chassis.core.rejection.DomainRejection._
import allawala.chassis.util.UserUtil
import com.typesafe.scalalogging.StrictLogging
import org.apache.shiro.subject.Subject

import scala.concurrent.Future

object RouteSecurity {
  val AccessControlExposeHeader = "Access-Control-Expose-Headers"
  val Authorization = "Authorization"
  val RefreshToken = "Refresh-Token"
  val Bearer = "Bearer"
}

trait RouteSecurity extends Directives with StrictLogging {
  import RouteSecurity._

  def authService: ShiroAuthService
  implicit def am: ActorMaterializer

  val onAuthenticated: Directive1[Subject] = {
    extractRequest tflatMap { request =>
      (headerValueByName(Authorization) & optionalHeaderValueByName(RefreshToken)).tflatMap {
        case (authToken, refreshToken) =>
          if (!authToken.startsWith("Bearer")) {
            discardRequest(request._1)
            reject(AuthenticationException(logMap = Map("reason" -> "Missing Bearer")))
          } else {
            val jwtToken = authToken.split(' ').last
            onSuccess(authService.authenticateToken(jwtToken, refreshToken)) flatMap {
              case Left(e) =>
                discardRequest(request._1)
                reject(e)
              case Right(authenticatedSubject) =>
                // new tokens issued
                if (authenticatedSubject.jwtToken != jwtToken) {
                  authenticated(authenticatedSubject)
                } else {
                  provide(authenticatedSubject.subject)
                }
            }
          }
      }
    }
  }

  val onInvalidateSession: Directive0 = {
    (headerValueByName(Authorization) & optionalHeaderValueByName(RefreshToken)).tflatMap {
      case (authToken, refreshToken) =>
        if (!authToken.startsWith("Bearer")) {
          reject(AuthenticationException(logMap = Map("reason" -> "Missing Bearer")))
        } else {
          val jwtToken = authToken.split(' ').last
          onSuccess(authService.invalidate(jwtToken, refreshToken)) flatMap {
            case Left(e) => reject(e)
            case Right(_) => pass
          }
        }
    }
  }

  val onInvalidateAllSessions: Directive0 = {
    headerValueByName(Authorization).flatMap { authToken =>
      if (!authToken.startsWith("Bearer")) {
        reject(AuthenticationException(logMap = Map("reason" -> "Missing Bearer")))
      } else {
        val jwtToken = authToken.split(' ').last
        onSuccess(authService.invalidateAll(jwtToken)) flatMap {
          case Left(e) => reject(e)
          case Right(_) => pass
        }
      }
    }
  }

  def authorized(subject: Subject, permission: String): Directive0 = {
    if (!authService.isAuthorizedSync(subject, permission)) {
      reject(AuthorizationException())
    }
    else {
      pass
    }
  }

  def onAuthorized(subject: Subject, permission: String): Directive0 = {
    onSuccess(authService.isAuthorized(subject, permission)).flatMap { authorized =>
      if (!authorized) {
        reject(AuthorizationException())
      }
      else {
        pass
      }
    }
  }

  def authorizedAny(subject: Subject, permissions: String*): Directive0 = {
    if (!authService.isAuthorizedAnySync(subject, permissions.toSet)) {
      reject(AuthorizationException())
    } else {
      pass
    }
  }

  def onAuthorizedAny(subject: Subject, permissions: String*): Directive0 = {
    onSuccess(authService.isAuthorizedAny(subject, permissions.toSet)).flatMap { authorized =>
      if (!authorized) {
        reject(AuthorizationException())
      } else {
        pass
      }
    }
  }

  def authorizedAll(subject: Subject, permissions: String*): Directive0 = {
    if (!authService.isAuthorizedAllSync(subject, permissions.toSet)) {
      reject(AuthorizationException())
    } else {
      pass
    }
  }

  def onAuthorizedAll(subject: Subject, permissions: String*): Directive0 = {
    onSuccess(authService.isAuthorizedAll(subject, permissions.toSet)).flatMap { authorized =>
      if (!authorized) {
        reject(AuthorizationException())
      } else {
        pass
      }
    }
  }

  /*
    Very simple authorization that checks if the incoming token is a service token or not, If so allows the request without checking any other permissions
   */
  def authorizedService(subject: Subject): Directive0 = {
    if (!UserUtil.isService(subject)) {
      reject(AuthorizationException(logMap = Map("reason" -> "service principal expected")))
    } else {
      pass
    }
  }

  def onAuthenticate(user: String, password: String, rememberMe: Boolean): Directive1[Subject] = {
    onAuthenticateWithFailureHandling(user, password, rememberMe) {
      Future.successful(Right(()))
    }
  }

  def onAuthenticateWithFailureHandling(user: String, password: String, rememberMe: Boolean)
                                       (onFailure: => ResponseFE[Unit]): Directive1[Subject] = {
    onSuccess(authService.authenticateCredentials(user, password, rememberMe)).flatMap {
      case Left(ex) => onSuccess(onFailure) flatMap {
        case Left(e) => reject(e) // Something in error handling itself gone wrong
        case Right(_) => reject(AuthenticationException(cause = ex))
      }
      case Right(authenticatedSubject) => authenticated(authenticatedSubject)
    }
  }

  private def authenticated(authenticatedSubject: AuthenticatedSubject): Directive1[Subject] = {
    setAuthorizationHeader(authenticatedSubject.jwtToken) &
      setRefreshHeader(authenticatedSubject.refreshToken) &
      provide(authenticatedSubject.subject)
  }

  private def setAuthorizationHeader(token: String): Directive0 = {
    val accessControlAccessHeader = RawHeader(AccessControlExposeHeader, Authorization)
    val authorizationHeader = RawHeader(Authorization, s"$Bearer $token")
    respondWithHeaders(accessControlAccessHeader, authorizationHeader)
  }

  private def setRefreshHeader(refreshToken: Option[String]): Directive0 = {
    refreshToken match {
      case Some(rt) =>
        val accessControlAccessHeader = RawHeader(AccessControlExposeHeader, RefreshToken)
        val refreshHeader = RawHeader(RefreshToken, rt)
        respondWithHeaders(accessControlAccessHeader, refreshHeader)
      case None => pass
    }
  }

  // Clean up the stream if needed as calling authenticate first and failing will short circuit entity from every being read
  private def discardRequest(request: HttpRequest) = {
    if (!request.entity.isKnownEmpty()) {
      request.discardEntityBytes()
    }
  }

  val jwtToken: Directive1[String] = headerValueByName(Authorization).map(header => header.split(' ').last)
}
