package allawala.chassis.auth.shiro.realm

import allawala.chassis.auth.shiro.model.JWTAuthenticationToken
import org.apache.shiro.authc.{AuthenticationInfo, AuthenticationToken, SimpleAccount}
import org.apache.shiro.authz.{AuthorizationInfo, SimpleAuthorizationInfo}
import org.apache.shiro.realm.AuthorizingRealm
import org.apache.shiro.subject.PrincipalCollection

import scala.collection.JavaConverters._

/*
  This class is just provided as an example of an Authorizing JWT realm. It is too permissive for any production environment as
  it allows everything through. Each micro service requiring authentication and authorization should implement a realm that
  checks for token invalidation and provides restrictive authorization.
 */
class JWTRealm extends AuthorizingRealm {

  override def supports(token: AuthenticationToken): Boolean = {
    Option(token) match {
      case Some(t) => t.isInstanceOf[JWTAuthenticationToken]
      case None => false
    }
  }

  /*
   This implementation does not perform any additional checks after the successful decoding of the JWT token. However micro
   services extending this chassis should implement a way to invalidate a token and perform a check here accordingly
  */
  override def doGetAuthenticationInfo(authenticationToken: AuthenticationToken): AuthenticationInfo = {
    val token = authenticationToken.asInstanceOf[JWTAuthenticationToken]
    new SimpleAccount(token.getPrincipal, token.getCredentials, getName)
  }

  override def doGetAuthorizationInfo(principals: PrincipalCollection): AuthorizationInfo = {
    val roleNames = Set.empty[String]
    val permissions = Set("*")

    val info = new SimpleAuthorizationInfo(roleNames.asJava)
    info.setStringPermissions(permissions.asJava)
    info
  }
}
