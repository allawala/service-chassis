package allawala.chassis.auth.shiro.service

import allawala.chassis.auth.shiro.model.AuthenticatedSubject
import allawala.{ResponseE, ResponseFE}
import org.apache.shiro.subject.Subject

import scala.concurrent.Future

trait ShiroAuthService {
  def authenticateCredentials(user: String, password: String, rememberMe: Boolean): ResponseFE[AuthenticatedSubject]
  def authenticateToken(jwtToken: String, refreshToken: Option[String]): ResponseFE[AuthenticatedSubject]

  def impersonateSubjectSync(principal: String, credentials: String): ResponseE[Subject]
  def impersonateSubject(principal: String, credentials: String): ResponseFE[Subject]

  def isAuthorizedSync(subject: Subject, permission: String): Boolean
  def isAuthorized(subject: Subject, permission: String): Future[Boolean]

  def isAuthorizedAnySync(subject: Subject, permissions: Set[String]): Boolean
  def isAuthorizedAny(subject: Subject, permissions: Set[String]): Future[Boolean]

  def isAuthorizedAllSync(subject: Subject, permissions: Set[String]): Boolean
  def isAuthorizedAll(subject: Subject, permissions: Set[String]): Future[Boolean]

  def invalidate(jwtToken: String, refreshToken: Option[String]): ResponseFE[Unit]
  def invalidateAll(jwtToken: String): ResponseFE[Unit]
}
