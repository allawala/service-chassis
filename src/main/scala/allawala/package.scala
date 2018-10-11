import allawala.chassis.core.exception.DomainException
import allawala.chassis.core.validation.ValidationError
import cats.data.ValidatedNel

import scala.concurrent.Future

package object allawala {
  type ResponseFE[T] = Future[Either[DomainException, T]]
  type ResponseE[T] = Either[DomainException, T]
  type ValidationResult[T] = ValidatedNel[ValidationError, T]
}
