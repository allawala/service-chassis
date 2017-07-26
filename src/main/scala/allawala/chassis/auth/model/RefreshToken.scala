package allawala.chassis.auth.model

case class RefreshToken(selector: String, tokenHash: String, expires: Long)
