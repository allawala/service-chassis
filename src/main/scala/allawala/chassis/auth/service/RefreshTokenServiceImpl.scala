package allawala.chassis.auth.service
import java.time.{Duration => JDuration}
import jakarta.inject.Inject

import allawala.chassis.auth.model.RefreshToken
import allawala.chassis.config.model.Auth
import allawala.chassis.util.{DataGenerator, DateTimeProvider}
import org.threeten.extra.Temporals

class RefreshTokenServiceImpl @Inject()(
                                         val auth: Auth,
                                         val dataGenerator: DataGenerator,
                                         val dateTimeProvider: DateTimeProvider
                                       ) extends RefreshTokenService {

  protected val SelectorLength = 16
  protected val TokenLength = 64

  override def generateToken(): RefreshToken = {
    val expiration = auth.expiration
    val selector = createSelector()
    val token = createToken()
    val tokenHash = hashToken(token)
    val duration = JDuration.of(expiration.refreshTokenExpiry._1, Temporals.chronoUnit(expiration.refreshTokenExpiry._2))
    val expires = dateTimeProvider.now.plus(duration)
    RefreshToken(selector, Some(encodeSelectorAndToken(selector, token)), tokenHash, expires)
  }

  override def hashToken(token: String): String = {
    dataGenerator.hashSHA256(token)
  }
  override def decodeSelectorAndToken(value: String): Option[(String, String)] = {
    val s = value.split(":", 2)
    if (s.length == 2) Some((s(0), s(1))) else None
  }

  protected def createSelector(): String = dataGenerator.randomString(SelectorLength)

  protected def createToken(): String = dataGenerator.randomString(TokenLength)

  protected def encodeSelectorAndToken(selector: String, token: String): String = s"$selector:$token"
}
