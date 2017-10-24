package allawala.chassis.config.model

import enumeratum.EnumEntry.Lowercase
import enumeratum._

sealed trait RefreshStrategy extends EnumEntry with Lowercase

case object RefreshStrategy extends Enum[RefreshStrategy] with CirceEnum[RefreshStrategy] {

  // Ignore the refresh token, Issue the bearer token but use the refresh token expiry as the expiry.
  case object Simple extends RefreshStrategy
  // Proper refresh token and remember me semantics
  case object Full extends RefreshStrategy

  def isSimple(strategy: String): Boolean = withNameOption(strategy).contains(Simple)
  def isFull(strategy: String): Boolean = withNameOption(strategy).contains(Full)

  val values = findValues
}