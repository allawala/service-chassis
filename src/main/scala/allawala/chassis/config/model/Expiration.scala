package allawala.chassis.config.model

import scala.concurrent.duration.FiniteDuration

case class Expiration(expiry: FiniteDuration, refreshTokenExpiry: FiniteDuration, refreshTokenStrategy: String)
