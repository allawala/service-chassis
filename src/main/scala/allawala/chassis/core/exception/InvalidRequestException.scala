package allawala.chassis.core.exception

import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import allawala.chassis.core.model.ErrorType

case class InvalidRequestException(
                                    override val errorCode: String = "client.error",
                                    override val cause: Throwable = None.orNull,
                                    override val messageParameters: Seq[AnyRef] = Seq.empty,
                                    override val logMap: Map[String, AnyRef] = Map.empty[String, AnyRef]
                                  ) extends DomainException {
  override val statusCode: StatusCode = StatusCodes.BadRequest
  override val errorType: ErrorType = ErrorType.ValidationError
}
