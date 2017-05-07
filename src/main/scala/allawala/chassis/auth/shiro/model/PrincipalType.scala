package allawala.chassis.auth.shiro.model

import enumeratum.{CirceEnum, Enum, EnumEntry}
import enumeratum.EnumEntry.Lowercase

sealed trait PrincipalType extends EnumEntry with Lowercase

case object PrincipalType extends Enum[PrincipalType] with CirceEnum[PrincipalType] {

  case object Service extends PrincipalType
  case object User extends PrincipalType

  val values = findValues
}