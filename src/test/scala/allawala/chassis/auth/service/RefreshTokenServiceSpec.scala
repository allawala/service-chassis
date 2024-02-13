package allawala.chassis.auth.service

import java.util.concurrent.TimeUnit

import allawala.chassis.common.{BaseSpec, DateTimeSpec}
import allawala.chassis.config.model.{Auth, Expiration}
import allawala.chassis.util.DataGenerator

import scala.concurrent.duration.FiniteDuration

class RefreshTokenServiceSpec extends BaseSpec with DateTimeSpec {
  private val auth = mock[Auth]
  private val expiration = mock[Expiration]
  private val dataGenerator = mock[DataGenerator]
  private val service = new RefreshTokenServiceImpl(auth, dataGenerator, dateTimeProvider)

  trait Fixture {
    val selector = "selector"
    val validator = "validator"
    val hashedToken = "hashedToken"

    auth.expiration returns expiration
    dataGenerator.randomString(any[Int]) returns(Seq(selector, validator))
    dataGenerator.hashSHA256(equ("validator")) returns hashedToken
    expiration.refreshTokenExpiry returns FiniteDuration(1, TimeUnit.MINUTES)
  }

  "refresh token service" should {
    "successfully generate token" in {
      new Fixture {
        val result = service.generateToken()

        result.encodedSelectorAndToken.isDefined shouldBe true
        result.encodedSelectorAndToken.get should ===(s"$selector:$validator")
        result.selector should===(selector)
        result.tokenHash should===(hashedToken)
        result.expires should===(later)
      }
    }
  }
}
