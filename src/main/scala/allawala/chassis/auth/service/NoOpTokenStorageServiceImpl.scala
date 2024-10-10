package allawala.chassis.auth.service

import jakarta.inject.Inject

import allawala.ResponseFE
import allawala.chassis.auth.model.{PrincipalType, RefreshToken}
import allawala.chassis.util.DateTimeProvider

import scala.concurrent.Future

class NoOpTokenStorageServiceImpl @Inject()(val dateTimeProvider: DateTimeProvider) extends TokenStorageService {
  override def storeTokens(principalType: PrincipalType, principal: String, jwtToken: String, refreshToken: Option[RefreshToken]): ResponseFE[Unit] = {
    Future.successful(Right(()))
  }

  override def lookupTokens(selector: String): ResponseFE[(String, RefreshToken)] = {
    Future.successful(Right(("jwtToken", RefreshToken("selector", None, "tokenHash", dateTimeProvider.now))))
  }

  override def rotateTokens(
                             principalType: PrincipalType, principal: String, oldJwtToken: String, jwtToken: String, oldRefreshToken: RefreshToken, refreshToken: RefreshToken
                           ): ResponseFE[Unit] = {
    Future.successful(Right(()))
  }

  override def removeTokens(principalType: PrincipalType, principal: String, jwtToken: String, refreshTokenSelector: Option[String]): ResponseFE[Unit] = {
    Future.successful(Right(()))
  }

  override def removeAllTokens(principalType: PrincipalType, principal: String): ResponseFE[Unit] = {
    Future.successful(Right(()))
  }
}
