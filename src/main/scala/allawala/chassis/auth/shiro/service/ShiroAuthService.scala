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

  def authenticate(authToken: JWTAuthenticationToken): Subject
  def authenticateAsync(authToken: JWTAuthenticationToken): Future[Subject]

  def isAuthorized(permission: String, subject: Subject): Boolean
  def isAuthorizedAsync(permission: String, subject: Subject): Future[Boolean]

  def authenticateCredentials(authToken: UsernamePasswordToken): Subject
  def authenticateCredentialsAsync(authToken: UsernamePasswordToken): Future[Subject]
}
