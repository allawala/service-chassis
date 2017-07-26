package allawala.chassis.auth.service

import allawala.chassis.auth.model.{RefreshToken, RefreshTokenLookupResult}

// TODO update the return types to possibly be either
trait RefreshTokenService {
  def lookup(selector: String): Option[RefreshTokenLookupResult]
  def store(refreshToken: RefreshToken): Unit
  def remove(selector: String): Unit
}
