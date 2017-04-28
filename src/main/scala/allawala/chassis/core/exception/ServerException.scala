package allawala.chassis.core.exception

import akka.http.scaladsl.model.{StatusCode, StatusCodes}

case class ServerException(
                            override val message: String = "server exception",
                            override val cause: Throwable = None.orNull) extends DomainException {
  override def errorType: ErrorType = ErrorType.ServerError
  override val statusCode: StatusCode = StatusCodes.InternalServerError
  override def errorCode: String = "server.error"
}
