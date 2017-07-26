package allawala.chassis.auth.shiro.model

case class JWTSubject(principalType: PrincipalType, principal: String, credentials: String)
