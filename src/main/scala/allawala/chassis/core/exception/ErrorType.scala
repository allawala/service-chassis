package allawala.chassis.core.exception

import enumeratum.{CirceEnum, Enum, EnumEntry}

import scala.collection.immutable

sealed trait ErrorType extends EnumEntry

case object ErrorType extends Enum[ErrorType] with CirceEnum[ErrorType] {

  case object ValidationError extends ErrorType

  case object ServerError extends ErrorType

  case object AccessDeniedError extends ErrorType

  case object UnknownError extends ErrorType

  val values: immutable.IndexedSeq[ErrorType] = findValues
}
