package allawala.chassis.auth.exception

import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import allawala.chassis.core.exception.DomainException
import allawala.chassis.core.model.ErrorType

case class AuthorizationException(
                                override val errorCode: String = "authorization.error",
                                override val cause: Throwable = None.orNull,
                                override val messageParameters: Seq[AnyRef] = Seq.empty,
                                override val logMap: Map[String, AnyRef] = Map.empty[String, AnyRef]
                              ) extends DomainException {

  override val errorType: ErrorType = ErrorType.AccessDeniedError
  override val statusCode: StatusCode = StatusCodes.Forbidden

}

