package allawala.chassis.auth.shiro.model

import enumeratum.{CirceEnum, Enum, EnumEntry}
import enumeratum.EnumEntry.Lowercase

sealed trait PrincipalType extends EnumEntry with Lowercase

case object PrincipalType extends Enum[PrincipalType] with CirceEnum[PrincipalType] {

  case object Service extends PrincipalType
  case object User extends PrincipalType
  /*
    Eg. "service" principal type enquires whether a certain user is permitted to perform some action. This allows us to have
    flexibility in defining our authentication logic differently for the two user types if required and allowing us to get a
    Shiro subject representing the impersonated user.
  */
  case object ImpersonatedUser extends PrincipalType

  val values = findValues
}