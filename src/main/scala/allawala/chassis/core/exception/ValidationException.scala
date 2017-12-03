package allawala.chassis.core.exception

import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import allawala.chassis.core.model.ErrorType
import allawala.chassis.core.validation.ValidationError
import cats.data.NonEmptyList

case class ValidationException(
                                validationErrors: NonEmptyList[ValidationError],
                                modelName: Option[String] = None
                              ) extends DomainException {
  override val statusCode: StatusCode = StatusCodes.BadRequest
  override val errorCode: String = s"validation.error"
  override val errorType: ErrorType = ErrorType.ValidationError
  override val cause: Throwable = None.orNull
}
