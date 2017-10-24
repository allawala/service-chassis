package allawala.chassis.auth.model

import enumeratum.EnumEntry.Lowercase
import enumeratum.{CirceEnum, Enum, EnumEntry}

sealed trait PrincipalType extends EnumEntry with Lowercase

case object PrincipalType extends Enum[PrincipalType] with CirceEnum[PrincipalType] {

  case object Service extends PrincipalType
  case object User extends PrincipalType
  /*
    Eg. "service" principal type enquires whether a certain user is permitted to perform some action. This allows us to have
    flexibility in defining our authentication logic differently for the two user types if required and allowing us to get a
    subject representing the impersonated user.
  */
  case object ImpersonatedUser extends PrincipalType

  val values = findValues
}