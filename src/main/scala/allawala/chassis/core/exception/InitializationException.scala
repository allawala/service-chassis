package allawala.chassis.core.exception

import allawala.chassis.DomainException

trait InitializationException extends DomainException {
  def message: String
  def cause: Throwable
  override def getMessage: String = message
  override def getCause: Throwable = cause
}
