package allawala.chassis.auth.shiro

import allawala.chassis.auth.model.{JWTSubject, PrincipalType, RefreshToken}
import allawala.chassis.auth.service.{JWTTokenService, RefreshTokenService, TokenStorageService}
import allawala.chassis.auth.shiro.model.JWTAuthenticationToken
import allawala.chassis.auth.shiro.service.ShiroAuthServiceImpl
import allawala.chassis.common.{BaseSpec, DateTimeSpec, FutureSpec}
import allawala.chassis.config.model.{Auth, Expiration, RefreshStrategy}
import allawala.chassis.core.exception.ServerException
import org.apache.shiro.authc.{UsernamePasswordToken, AuthenticationException => ShiroAuthenticationException}
import org.apache.shiro.subject.Subject

import scala.concurrent.{Await, Future}

class ShiroAuthServiceSpec extends BaseSpec with FutureSpec with DateTimeSpec {
  private val subject = mock[Subject]
  private val auth = mock[Auth]
  private val expiration = mock[Expiration]
  private val jwtTokenService = mock[JWTTokenService]
  private val refreshTokenService = mock[RefreshTokenService]
  private val tokenStorageService = mock[TokenStorageService]

  before {
    reset(subject, auth, expiration, jwtTokenService, refreshTokenService, tokenStorageService)
  }

  private val service =
    new ShiroAuthServiceImpl(auth, dateTimeProvider, jwtTokenService, refreshTokenService, tokenStorageService) {
      override def buildSubject: Subject = subject
    }

  trait Fixture {
    val user = "user"
    val jwtToken = "jwtToken"
    val newJwtToken = "newJwtToken"
    val refreshToken = RefreshToken("selector", Some("encoded"), "tokenHash", now)
    val newRefreshToken = RefreshToken("newSelector", Some("newEncoded"), "newTokenHash", later)
  }

  trait UserNamePasswordAuthFixture extends Fixture {
    val password = "password"
    auth.expiration returns expiration
    jwtTokenService.generateToken(equ(PrincipalType.User), equ(user), any[Boolean]) returns jwtToken
  }

  trait JWTTokenAuthFixture extends Fixture {
    val encodedRefreshToken = "refreshToken"
    val jwtSubject = JWTSubject(PrincipalType.User, "user", jwtToken)
  }

  "shiro auth service (User Authentication)" should {

    "successfully authenticate user credentials without remember me" in {
      new UserNamePasswordAuthFixture {
        tokenStorageService.storeTokens(
          equ(PrincipalType.User), equ(user), equ(jwtToken), equ(None)
        ) returns Future.successful(Right(()))

        val result = Await.result(service.authenticateCredentials(user, password, rememberMe = false), timeout).toOption.get

        result.subject should ===(subject)
        result.jwtToken should ===(jwtToken)
        result.refreshToken.isEmpty shouldBe true

        oneOf(subject).login(any[UsernamePasswordToken])
        noneOf(refreshTokenService).generateToken()
      }
    }

    "successfully authenticate user credentials with remember me and refresh strategy simple" in {
      new UserNamePasswordAuthFixture {
        expiration.refreshTokenStrategy returns RefreshStrategy.Simple.entryName
        tokenStorageService.storeTokens(
          equ(PrincipalType.User), equ(user), equ(jwtToken), equ(None)
        ) returns Future.successful(Right(()))

        val result = Await.result(service.authenticateCredentials(user, password, rememberMe = true), timeout).toOption.get

        result.subject should ===(subject)
        result.jwtToken should ===(jwtToken)
        result.refreshToken.isEmpty shouldBe true

        oneOf(subject).login(any[UsernamePasswordToken])
        noneOf(refreshTokenService).generateToken()
      }
    }

    "successfully authenticate user credentials with remember me and refresh strategy full" in {
      new UserNamePasswordAuthFixture {
        refreshTokenService.generateToken() returns refreshToken
        expiration.refreshTokenStrategy returns RefreshStrategy.Full.entryName
        tokenStorageService.storeTokens(
          equ(PrincipalType.User), equ(user), equ(jwtToken), equ(Some(refreshToken))
        ) returns Future.successful(Right(()))

        val result = Await.result(service.authenticateCredentials(user, password, rememberMe = true), timeout).toOption.get

        result.subject should ===(subject)
        result.jwtToken should ===(jwtToken)
        result.refreshToken should ===(Some("encoded"))

        oneOf(subject).login(any[UsernamePasswordToken])
      }
    }

    "fail to authenticate user credentials if storing tokens fail" in {
      new UserNamePasswordAuthFixture {
        tokenStorageService.storeTokens(
          equ(PrincipalType.User), equ(user), equ(jwtToken), any[Option[RefreshToken]]
        ) returns Future.successful(Left(ServerException()))

        val result = Await.result(service.authenticateCredentials(user, password, rememberMe = true), timeout)

        result.isLeft shouldBe true
        oneOf(subject).login(any[UsernamePasswordToken])
      }
    }

    "fail to authenticate user credentials if shiro authentication fails" in {
      new UserNamePasswordAuthFixture {
        subject.login(any[UsernamePasswordToken]) throws new ShiroAuthenticationException("failure")

        val result = Await.result(service.authenticateCredentials(user, password, rememberMe = true), timeout)

        result.isLeft shouldBe true
        noneOf(tokenStorageService).storeTokens(any[PrincipalType], any[String], any[String], any[Option[RefreshToken]])
      }
    }
  }


  "shiro auth service (Token Authentication)" should {

    "successfully authenticate (valid jwt token, no refresh token)" in {
      new JWTTokenAuthFixture {
        jwtTokenService.decodeToken(jwtToken) returns Right(jwtSubject)

        val result = Await.result(service.authenticateToken(jwtToken, None), timeout).toOption.get

        result.subject should ===(subject)
        result.jwtToken shouldBe jwtToken
        result.refreshToken.isEmpty shouldBe true

        oneOf(subject).login(equ(JWTAuthenticationToken(jwtSubject)))
      }
    }

    "successfully authenticate (valid jwt token, refresh token)" in {
      new JWTTokenAuthFixture {
        jwtTokenService.decodeToken(jwtToken) returns Right(jwtSubject)

        val result = Await.result(service.authenticateToken(jwtToken, Some(encodedRefreshToken)), timeout).toOption.get

        result.subject should ===(subject)
        result.jwtToken shouldBe jwtToken
        result.refreshToken.isEmpty shouldBe true

        oneOf(subject).login(equ(JWTAuthenticationToken(jwtSubject)))
      }
    }

    "fail to authenticate an invalid jwt token" in {
      new JWTTokenAuthFixture {
        jwtTokenService.decodeToken(jwtToken) returns Left(ServerException())
        jwtTokenService.decodeExpiredToken(jwtToken) returns Left(ServerException())

        val result = Await.result(service.authenticateToken(jwtToken, None), timeout)

        result.isLeft shouldBe true

        noneOf(subject).login(any[JWTAuthenticationToken])
      }
    }

    "fail to authenticate (expired jwt token, no refresh token)" in {
      new JWTTokenAuthFixture {
        jwtTokenService.decodeToken(jwtToken) returns Left(ServerException())
        jwtTokenService.decodeExpiredToken(jwtToken) returns Right(jwtSubject)
        tokenStorageService.removeTokens(
          equ(PrincipalType.User), equ(user), equ(jwtToken), equ(None)
        ) returns Future.successful(Right(()))

        val result = Await.result(service.authenticateToken(jwtToken, None), timeout)

        result.isLeft shouldBe true

        noneOf(subject).login(any[JWTAuthenticationToken])
      }
    }

    "successfully authenticate (expired jwt token, valid refresh token) and reissues tokens" in {
      new JWTTokenAuthFixture {
        jwtTokenService.decodeToken(jwtToken) returns Left(ServerException())
        jwtTokenService.decodeExpiredToken(jwtToken) returns Right(jwtSubject)
        refreshTokenService.decodeSelectorAndToken(equ(encodedRefreshToken)) returns Some(("selector", "validator"))
        tokenStorageService.lookupTokens(equ("selector")) returns Future.successful(Right((jwtToken, refreshToken)))
        refreshTokenService.hashToken(equ("validator")) returns "tokenHash"
        refreshTokenService.generateToken() returns newRefreshToken
        jwtTokenService.generateToken(equ(PrincipalType.User), equ(user), equ(true)) returns newJwtToken
        tokenStorageService.rotateTokens(
          equ(PrincipalType.User), equ(user), equ(jwtToken), equ(newJwtToken), equ(refreshToken), equ(newRefreshToken)
        ) returns Future.successful(Right(()))

        val result = Await.result(service.authenticateToken(jwtToken, Some(encodedRefreshToken)), timeout).toOption.get

        result.subject should ===(subject)
        result.jwtToken shouldBe newJwtToken
        result.refreshToken should ===(newRefreshToken.encodedSelectorAndToken)

        oneOf(subject).login(equ(JWTAuthenticationToken(jwtSubject)))
      }
    }

    "fail to authenticate (expired jwt token, expired refresh token)" in {
      new JWTTokenAuthFixture {
        val expiredRefreshToken = refreshToken.copy(expires = earlier)
        jwtTokenService.decodeToken(jwtToken) returns Left(ServerException())
        jwtTokenService.decodeExpiredToken(jwtToken) returns Right(jwtSubject)
        refreshTokenService.decodeSelectorAndToken(equ(encodedRefreshToken)) returns Some(("selector", "validator"))
        tokenStorageService.lookupTokens(equ("selector")) returns Future.successful(Right((jwtToken, expiredRefreshToken)))
        tokenStorageService.removeTokens(
          equ(PrincipalType.User), equ(user), equ(jwtToken), equ(Some("selector"))
        ) returns Future.successful(Right(()))

        val result = Await.result(service.authenticateToken(jwtToken, Some(encodedRefreshToken)), timeout)

        result.isLeft shouldBe true
        noneOf(refreshTokenService).hashToken(any[String])
        noneOf(refreshTokenService).generateToken()
        noneOf(jwtTokenService).generateToken(any[PrincipalType], any[String], any[Boolean])
        noneOf(tokenStorageService).rotateTokens(
          any[PrincipalType], any[String], any[String], any[String], any[RefreshToken], any[RefreshToken]
        )
        noneOf(subject).login(equ(JWTAuthenticationToken(jwtSubject)))
      }
    }

    "fail to authenticate (expired jwt token, valid refresh token) if shiro auth fails" in {
      new JWTTokenAuthFixture {
        jwtTokenService.decodeToken(jwtToken) returns Left(ServerException())
        jwtTokenService.decodeExpiredToken(jwtToken) returns Right(jwtSubject)
        refreshTokenService.decodeSelectorAndToken(equ(encodedRefreshToken)) returns Some(("selector", "validator"))
        tokenStorageService.lookupTokens(equ("selector")) returns Future.successful(Right((jwtToken, refreshToken)))
        refreshTokenService.hashToken(equ("validator")) returns "tokenHash"
        subject.login(equ(JWTAuthenticationToken(jwtSubject))) throws new ShiroAuthenticationException("failure")

        val result = Await.result(service.authenticateToken(jwtToken, Some(encodedRefreshToken)), timeout)

        result.isLeft shouldBe true

        noneOf(tokenStorageService).rotateTokens(
          any[PrincipalType], any[String], any[String], any[String], any[RefreshToken], any[RefreshToken]
        )
      }
    }

    "fail to authenticate (expired jwt token, refresh token) if refresh token cannot be decoded" in {
      new JWTTokenAuthFixture {
        jwtTokenService.decodeToken(jwtToken) returns Left(ServerException())
        jwtTokenService.decodeExpiredToken(jwtToken) returns Right(jwtSubject)
        refreshTokenService.decodeSelectorAndToken(equ(encodedRefreshToken)) returns None
        tokenStorageService.removeTokens(
          equ(PrincipalType.User), equ(user), equ(jwtToken), equ(None)
        ) returns Future.successful(Right(()))

        val result = Await.result(service.authenticateToken(jwtToken, Some(encodedRefreshToken)), timeout)

        result.isLeft shouldBe true

        noneOf(tokenStorageService).lookupTokens(any[String])
        noneOf(refreshTokenService).hashToken(any[String])
        noneOf(refreshTokenService).generateToken()
        noneOf(jwtTokenService).generateToken(any[PrincipalType], any[String], any[Boolean])
        noneOf(tokenStorageService).rotateTokens(
          any[PrincipalType], any[String], any[String], any[String], any[RefreshToken], any[RefreshToken]
        )
        noneOf(subject).login(any[JWTAuthenticationToken])
      }
    }

    "fail to authenticate (expired jwt token, refresh token) if refresh token cannot be looked up" in {
      new JWTTokenAuthFixture {
        jwtTokenService.decodeToken(jwtToken) returns Left(ServerException())
        jwtTokenService.decodeExpiredToken(jwtToken) returns Right(jwtSubject)
        refreshTokenService.decodeSelectorAndToken(equ(encodedRefreshToken)) returns Some(("selector", "validator"))
        tokenStorageService.lookupTokens(equ("selector")) returns Future.successful(Left(ServerException()))
        tokenStorageService.removeTokens(
          equ(PrincipalType.User), equ(user), equ(jwtToken), equ(None)
        ) returns Future.successful(Right(()))

        val result = Await.result(service.authenticateToken(jwtToken, Some(encodedRefreshToken)), timeout)

        result.isLeft shouldBe true

        noneOf(refreshTokenService).hashToken(any[String])
        noneOf(refreshTokenService).generateToken()
        noneOf(jwtTokenService).generateToken(any[PrincipalType], any[String], any[Boolean])
        noneOf(tokenStorageService).rotateTokens(
          any[PrincipalType], any[String], any[String], any[String], any[RefreshToken], any[RefreshToken]
        )
        noneOf(subject).login(any[JWTAuthenticationToken])
      }
    }

    "fail to authenticate (expired jwt token, valid refresh token) if refresh token hash does not match" in {
      new JWTTokenAuthFixture {
        jwtTokenService.decodeToken(jwtToken) returns Left(ServerException())
        jwtTokenService.decodeExpiredToken(jwtToken) returns Right(jwtSubject)
        refreshTokenService.decodeSelectorAndToken(equ(encodedRefreshToken)) returns Some(("selector", "validator"))
        tokenStorageService.lookupTokens(equ("selector")) returns Future.successful(Right((jwtToken, refreshToken)))
        refreshTokenService.hashToken(equ("validator")) returns "SometokenHash"
        tokenStorageService.removeTokens(
          equ(PrincipalType.User), equ(user), equ(jwtToken), equ(Some("selector"))
        ) returns Future.successful(Right(()))

        val result = Await.result(service.authenticateToken(jwtToken, Some(encodedRefreshToken)), timeout)

        result.isLeft shouldBe true

        noneOf(refreshTokenService).generateToken()
        noneOf(jwtTokenService).generateToken(any[PrincipalType], any[String], any[Boolean])
        noneOf(tokenStorageService).rotateTokens(
          any[PrincipalType], any[String], any[String], any[String], any[RefreshToken], any[RefreshToken]
        )
        noneOf(subject).login(any[JWTAuthenticationToken])
      }
    }

    "fail to authenticate (expired jwt token, valid refresh token) if token rotation fails" in {
      new JWTTokenAuthFixture {
        jwtTokenService.decodeToken(jwtToken) returns Left(ServerException())
        jwtTokenService.decodeExpiredToken(jwtToken) returns Right(jwtSubject)
        refreshTokenService.decodeSelectorAndToken(equ(encodedRefreshToken)) returns Some(("selector", "validator"))
        tokenStorageService.lookupTokens(equ("selector")) returns Future.successful(Right((jwtToken, refreshToken)))
        refreshTokenService.hashToken(equ("validator")) returns "tokenHash"
        refreshTokenService.generateToken() returns newRefreshToken
        jwtTokenService.generateToken(equ(PrincipalType.User), equ(user), equ(true)) returns newJwtToken
        tokenStorageService.rotateTokens(
          equ(PrincipalType.User), equ(user), equ(jwtToken), equ(newJwtToken), equ(refreshToken), equ(newRefreshToken)
        ) returns Future.successful(Left(ServerException()))

        val result = Await.result(service.authenticateToken(jwtToken, Some(encodedRefreshToken)), timeout)

        result.isLeft shouldBe true

        oneOf(subject).login(equ(JWTAuthenticationToken(jwtSubject)))
      }
    }

    "successfully invalidate a session (valid jwt token, no refresh token)" in {
      new JWTTokenAuthFixture {
        jwtTokenService.decodeExpiredToken(jwtToken) returns Right(jwtSubject)
        tokenStorageService.removeTokens(
          equ(PrincipalType.User), equ(user), equ(jwtToken), equ(None)
        ) returns Future.successful(Right(()))

        val result = Await.result(service.invalidate(jwtToken, None), timeout)

        result.isRight shouldBe true

        noneOf(subject).login(equ(JWTAuthenticationToken(jwtSubject)))
      }
    }

    "successfully invalidate a session (valid jwt token, valid refresh token)" in {
      new JWTTokenAuthFixture {
        jwtTokenService.decodeExpiredToken(jwtToken) returns Right(jwtSubject)
        refreshTokenService.decodeSelectorAndToken(equ(encodedRefreshToken)) returns Some(("selector", "validator"))
        tokenStorageService.removeTokens(
          equ(PrincipalType.User), equ(user), equ(jwtToken), equ(Some("selector"))
        ) returns Future.successful(Right(()))

        val result = Await.result(service.invalidate(jwtToken, Some(encodedRefreshToken)), timeout)

        result.isRight shouldBe true

        noneOf(subject).login(equ(JWTAuthenticationToken(jwtSubject)))
      }
    }

    "successfully invalidate a session (valid jwt token, invalid refresh token)" in {
      new JWTTokenAuthFixture {
        jwtTokenService.decodeExpiredToken(jwtToken) returns Right(jwtSubject)
        refreshTokenService.decodeSelectorAndToken(equ(encodedRefreshToken)) returns None
        tokenStorageService.removeTokens(
          equ(PrincipalType.User), equ(user), equ(jwtToken), equ(None)
        ) returns Future.successful(Right(()))

        val result = Await.result(service.invalidate(jwtToken, Some(encodedRefreshToken)), timeout)

        result.isRight shouldBe true

        noneOf(subject).login(equ(JWTAuthenticationToken(jwtSubject)))
      }
    }

    "successfully invalidate all sessions" in {
      new JWTTokenAuthFixture {
        jwtTokenService.decodeExpiredToken(jwtToken) returns Right(jwtSubject)
        tokenStorageService.removeAllTokens(equ(PrincipalType.User), equ(user)) returns Future.successful(Right(()))

        val result = Await.result(service.invalidateAll(jwtToken), timeout)

        result.isRight shouldBe true

        noneOf(subject).login(equ(JWTAuthenticationToken(jwtSubject)))
      }
    }
  }
}
