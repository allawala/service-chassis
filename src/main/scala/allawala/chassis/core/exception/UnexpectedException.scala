package allawala.chassis.core.exception

import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import allawala.chassis.core.model.ErrorType

/*
  Used for any uncaught exceptions. Should not be used for explicit domain exceptions
 */
case class UnexpectedException(
                                override val errorCode: String = "unexpected.error",
                                override val cause: Throwable) extends DomainException {
  override val errorType: ErrorType = ErrorType.UnknownError
  override val statusCode: StatusCode = StatusCodes.InternalServerError
  override val thread: Option[String] = None
}
