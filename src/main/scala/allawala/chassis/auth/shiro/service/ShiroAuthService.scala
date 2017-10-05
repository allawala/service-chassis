package allawala.chassis.auth.shiro.service

import java.time.Duration
import java.time.temporal.TemporalAmount

import allawala.chassis.auth.shiro.model.{JWTAuthenticationToken, JWTSubject, PrincipalType}
import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.subject.Subject

import scala.concurrent.Future

trait ShiroAuthService {
  // TODO get expiration from the config
  private val ThirtyDays = Duration.ofDays(30)
  def generateToken(principalType: PrincipalType, principal: String, expiresIn: TemporalAmount = ThirtyDays): String
  def decodeToken(token: String, refreshToken: Option[String]): JWTSubject
  def canDecodeToken(token: String): Boolean

  def authenticate(authToken: JWTAuthenticationToken): Subject
  def authenticateAsync(authToken: JWTAuthenticationToken): Future[Subject]

  def isAuthorized(subject: Subject, permission: String): Boolean
  def isAuthorizedAsync(subject: Subject, permission: String): Future[Boolean]

  def isAuthorizedAny(subject: Subject, permissions: Set[String]): Boolean
  def isAuthorizedAnyAsync(subject: Subject, permissions: Set[String]): Future[Boolean]

  def isAuthorizedAll(subject: Subject, permissions: Set[String]): Boolean
  def isAuthorizedAllAsync(subject: Subject, permissions: Set[String]): Future[Boolean]

  def authenticateCredentials(authToken: UsernamePasswordToken): Subject
  def authenticateCredentialsAsync(authToken: UsernamePasswordToken): Future[Subject]
}
