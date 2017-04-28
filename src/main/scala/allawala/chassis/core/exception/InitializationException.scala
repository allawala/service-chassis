package allawala.chassis.core.exception

import akka.http.scaladsl.model.{StatusCode, StatusCodes}

trait InitializationException extends DomainException {
  override val statusCode: StatusCode = StatusCodes.InternalServerError
  override val errorType: ErrorType = ErrorType.ServerError
}