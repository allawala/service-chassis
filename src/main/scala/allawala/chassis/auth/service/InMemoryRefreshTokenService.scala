package allawala.chassis.auth.service
import allawala.chassis.auth.model.{RefreshToken, RefreshTokenLookupResult}

// TODO implement
class InMemoryRefreshTokenService extends RefreshTokenService {
  override def lookup(selector: String): Option[RefreshTokenLookupResult] = None

  override def store(refreshToken: RefreshToken): Unit = {}

  override def remove(selector: String): Unit = {}
}
