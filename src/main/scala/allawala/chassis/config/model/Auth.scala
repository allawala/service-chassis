package allawala.chassis.config.model

case class RSA(publicKey: String, privateKey: String)

case class Auth(rsa: RSA)
