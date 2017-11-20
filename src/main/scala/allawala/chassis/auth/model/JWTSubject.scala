package allawala.chassis.auth.model

case class JWTSubject(principalType: PrincipalType, principal: String, credentials: String)
