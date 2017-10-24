package allawala.chassis.auth.service

import allawala.chassis.auth.model.RefreshToken

trait RefreshTokenService {
  def generateToken(): RefreshToken
  def hashToken(token: String): String
  def decodeSelectorAndToken(value: String): Option[(String, String)]
}
