package allawala.chassis.auth.service

import allawala.ResponseFE
import allawala.chassis.auth.model.{PrincipalType, RefreshToken}

trait TokenStorageService {
  def storeTokens(
                    principalType: PrincipalType, principal: String, jwtToken: String, refreshToken: Option[RefreshToken]
                  ): ResponseFE[Unit]

  /*
    Lookup the jwtToken and its associated refresh token
   */
  def lookupTokens(selector: String): ResponseFE[(String, RefreshToken)]

  /*
    Rotating tokens mean we have to have refresh tokens
   */
  def rotateTokens(
                     principalType: PrincipalType, principal: String,
                     oldJwtToken: String, jwtToken: String,
                     oldRefreshToken: RefreshToken, refreshToken: RefreshToken
                  ): ResponseFE[Unit]

  def removeTokens(
                    principalType: PrincipalType, principal: String, jwtToken: String, refreshTokenSelector: Option[String]
                  ): ResponseFE[Unit]

  def removeAllTokens(principalType: PrincipalType, principal: String): ResponseFE[Unit]
}
