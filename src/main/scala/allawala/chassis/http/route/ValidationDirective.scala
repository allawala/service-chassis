package allawala.chassis.http.route

import akka.http.scaladsl.server.{Directive1, Directives}
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import allawala.chassis.core.exception.ValidationException
import allawala.chassis.core.rejection.DomainRejection._
import allawala.chassis.core.validation.ValidationError
import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyList, Validated}

trait ValidationDirective extends Directives {
  def model[T](um: FromRequestUnmarshaller[T])(f: T => Validated[NonEmptyList[ValidationError], T]): Directive1[T] = {
    entity(um).flatMap { model =>
      validate(f(model))
    }
  }

  private def validate[T](f: => Validated[NonEmptyList[ValidationError], T]): Directive1[T] = {
    f match {
      case Invalid(e) => reject(ValidationException(e))
      case Valid(validated) => provide(validated)
    }
  }

}
