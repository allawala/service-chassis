package allawala.chassis.auth.shiro.model

import org.apache.shiro.subject.Subject

case class AuthenticatedSubject(subject: Subject, jwtToken: String, refreshToken: Option[String] = None)
