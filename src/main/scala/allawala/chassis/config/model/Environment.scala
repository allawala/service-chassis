package allawala.chassis.config.model

import enumeratum.EnumEntry.Lowercase
import enumeratum._

sealed trait Environment extends EnumEntry with Lowercase

case object Environment extends Enum[Environment] with CirceEnum[Environment] {

  case object Local extends Environment
  case object Dev extends Environment
  case object Staging extends Environment
  case object Sandbox extends Environment
  case object UAT extends Environment
  case object Production extends Environment

  val values = findValues
}