package allawala.chassis.auth.shiro.realm

import org.apache.shiro.authc.{AuthenticationInfo, AuthenticationToken, SimpleAccount, UsernamePasswordToken}
import org.apache.shiro.authz.{AuthorizationInfo, SimpleAuthorizationInfo}
import org.apache.shiro.realm.AuthorizingRealm
import org.apache.shiro.subject.PrincipalCollection
import scala.collection.JavaConverters._

/*
  This class is just provided as a placeholder realm as the actual authentication will be specific to the implementing micro
  service. It is too permissive for any production environment.
 */
class UsernamePasswordRealm extends AuthorizingRealm {
  override def supports(token: AuthenticationToken): Boolean = {
    Option(token) match {
      case Some(t) => t.isInstanceOf[UsernamePasswordToken]
      case None => false
    }
  }

  /*
    This sets the email/username as the principal, however it might be more useful to set the uuid as the principal
   */
  override def doGetAuthenticationInfo(authenticationToken: AuthenticationToken): AuthenticationInfo = {
    val token = authenticationToken.asInstanceOf[UsernamePasswordToken]
    new SimpleAccount(token.getPrincipal, token.getCredentials, getName)
  }

  override def doGetAuthorizationInfo(principals: PrincipalCollection): AuthorizationInfo = {
    val roleNames = Set.empty[String]
    val permissions = Set.empty[String]

    val info = new SimpleAuthorizationInfo(roleNames.asJava)
    info.setStringPermissions(permissions.asJava)
    info

  }
}
