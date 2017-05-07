package allawala.chassis.auth.shiro.model

import org.apache.shiro.authc.AuthenticationToken

case class Principal(principalType: PrincipalType, principal: String)

case class JWTAuthenticationToken(subject: JWTSubject) extends AuthenticationToken {
  override def getPrincipal: AnyRef = Principal(subject.principalType, subject.principal)
  override def getCredentials: AnyRef = subject.credentials
}
