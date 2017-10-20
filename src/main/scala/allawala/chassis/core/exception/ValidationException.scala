package allawala.chassis.core.exception

import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import allawala.chassis.core.model.ErrorType
import allawala.chassis.core.validation.ValidationError
import cats.data.NonEmptyList

case class ValidationException(clazz: Class[_], validationErrors: NonEmptyList[ValidationError]) extends DomainException {
  private val name = clazz.getSimpleName
  override val statusCode: StatusCode = StatusCodes.BadRequest
  override val errorCode: String = s"${name.toLowerCase}.validation.failed"
  override val message: String = s"$name validation failed"
  override val errorMap: Map[String, String] = validationErrors.map(e => e.code -> e.message).toList.toMap
  override val errorType: ErrorType = ErrorType.ValidationError
  override val cause: Throwable = None.orNull
}
