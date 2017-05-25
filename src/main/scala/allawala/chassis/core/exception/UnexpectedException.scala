package allawala.chassis.core.exception

import akka.http.scaladsl.model.{StatusCode, StatusCodes}

case class UnexpectedException(
                                override val errorCode: String = "unexpected.error",
                                override val cause: Throwable) extends DomainException {
  override val message: String = Option(cause.getMessage).getOrElse("unexpected exception")
  override val errorType: ErrorType = ErrorType.UnknownError
  override val statusCode: StatusCode = StatusCodes.InternalServerError
  override val thread: Option[String] = None
}
