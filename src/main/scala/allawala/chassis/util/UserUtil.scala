package allawala.chassis.util

import allawala.chassis.auth.model.PrincipalType
import allawala.chassis.auth.shiro.model.Principal
import org.apache.shiro.subject.Subject

object UserUtil {
  def isService(subject: Subject): Boolean = {
    val principal = subject.getPrincipal.asInstanceOf[Principal]
    principal.principalType == PrincipalType.Service
  }
}
