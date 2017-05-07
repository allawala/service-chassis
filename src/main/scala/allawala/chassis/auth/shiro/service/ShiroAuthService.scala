package allawala.chassis.auth.shiro.service

import allawala.chassis.auth.shiro.model.{JWTAuthenticationToken, JWTSubject}
import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.subject.Subject

import scala.concurrent.Future

trait ShiroAuthService {
  def generateToken(principal: JWTSubject): String
  def decodeToken(token: String, refreshToken: Option[String]): JWTSubject

  def authenticate(authToken: JWTAuthenticationToken): Subject
  def authenticateAsync(authToken: JWTAuthenticationToken): Future[Subject]

  def isAuthorized(permission: String, subject: Subject): Boolean
  def isAuthorizedAsync(permission: String, subject: Subject): Future[Boolean]

  def authenticateCredentials(authToken: UsernamePasswordToken): Subject
  def authenticateCredentialsAsync(authToken: UsernamePasswordToken): Future[Subject]
}
