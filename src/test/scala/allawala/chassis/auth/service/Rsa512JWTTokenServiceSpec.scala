package allawala.chassis.auth.service

import java.time.{Duration, Instant}

import allawala.chassis.auth.model.PrincipalType
import allawala.chassis.common.{BaseSpec, DateTimeSpec, RSASpec}
import allawala.chassis.config.model.{Auth, RSA}

class Rsa512JWTTokenServiceSpec extends BaseSpec with DateTimeSpec with RSASpec {
  private val auth = mock[Auth]
  private val rsa = mock[RSA]
  private val service = new Rsa512JWTTokenServiceImpl(auth, dateTimeProvider)

  trait Fixture {
    // decoding that happens internally in the jwt library looks at current time
    dateTimeProvider.now returns Instant.now
    auth.rsa returns rsa
    rsa.privateKey returns privateKey
    rsa.publicKey returns publicKey
  }

  "Rsa512 token service" should {
    "successfully generate token" in {
      new Fixture {
        // Should not throw any exceptions
        service.generateToken(PrincipalType.User, "uuid", Duration.ofDays(1L))
      }
    }

    "successfully decode a valid token" in {
      new Fixture {
        val token = service.generateToken(PrincipalType.User, "uuid", Duration.ofDays(1L))

        val result = service.decodeToken(token).toOption.get

        result.principalType should ===(PrincipalType.User)
        result.principal should ===("uuid")
        result.credentials should ===(token)
      }
    }

    "fail to validate an expired token" in {
      new Fixture {
        val token = service.generateToken(PrincipalType.User, "uuid", Duration.ofDays(-1L))

        val result = service.decodeToken(token)

        result.isLeft shouldBe true
      }
    }

    "successfully decode an expired token" in {
      new Fixture {
        val token = service.generateToken(PrincipalType.User, "uuid", Duration.ofDays(-1L))

        val result = service.decodeExpiredToken(token).toOption.get

        result.principalType should ===(PrincipalType.User)
        result.principal should ===("uuid")
        result.credentials should ===(token)
      }
    }

    "successfully returns isExpired as true for an expired token" in {
      new Fixture {
        val token = service.generateToken(PrincipalType.User, "uuid", Duration.ofDays(-1L))

        val result = service.isExpired(token)

        result shouldBe true
      }
    }

    "successfully returns isExpired as false for valid token" in {
      new Fixture {
        val token = service.generateToken(PrincipalType.User, "uuid", Duration.ofDays(1L))

        val result = service.isExpired(token)

        result shouldBe false
      }
    }
  }
}
