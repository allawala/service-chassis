package allawala.chassis.http.route

import akka.http.scaladsl.server.{Directive1, Directives}
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import allawala.ValidationResult
import allawala.chassis.core.exception.ValidationException
import allawala.chassis.core.rejection.DomainRejection._
import cats.data.Validated.{Invalid, Valid}

trait ValidationDirective extends Directives {
  def model[T](um: FromRequestUnmarshaller[T])(f: T => ValidationResult[T]): Directive1[T] = {
    entity(um).flatMap { model =>
      validate(f(model))
    }
  }

  private def validate[T](f: => ValidationResult[T]): Directive1[T] = {
    f match {
      case Invalid(e) => reject(ValidationException(e))
      case Valid(validated) => provide(validated)
    }
  }

}
