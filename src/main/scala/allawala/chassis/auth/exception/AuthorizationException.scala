package allawala.chassis.auth.exception

import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import allawala.chassis.core.exception.{DomainException, ErrorType}

case class AuthorizationException(
                                override val errorCode: String = "authorization.error",
                                override val message: String = "authorization exception",
                                override val cause: Throwable = None.orNull,
                                override val errorMap: Map[String, String] = Map.empty[String, String],
                                override val logMap: Map[String, AnyRef] = Map.empty[String, AnyRef]
                              ) extends DomainException {

  override val errorType: ErrorType = ErrorType.AccessDeniedError
  override val statusCode: StatusCode = StatusCodes.Unauthorized

}

