package allawala.chassis.core.validation

import allawala.ValidationResult
import cats.implicits._

trait ValidateRequired {
  protected def required[T](name: String, value: Option[T]): ValidationResult[T] = value match {
    case Some(v) => v.validNel
    case None => RequiredField(name).invalidNel
  }

  protected def requiredString(name: String, value: Option[String]): ValidationResult[String] =
    required[String](name, value)
}

trait ValidateUnexpected {
  protected def unexpected[T](name: String, value: Option[T]): ValidationResult[Option[T]] = value match {
    case Some(v) => UnexpectedField(name).invalidNel
    case None => value.validNel
  }

  protected def unexpectedString(name: String, value: Option[String]): ValidationResult[Option[String]] =
    unexpected[String](name, value)
}

trait ValidateNotBlank {
  protected def notBlank(name: String, value: String): ValidationResult[String] =
    if (value.trim.isEmpty) NotBlank(name).invalidNel else value.validNel

  protected def notBlank(name: String, value: Option[String]): ValidationResult[Option[String]] = value match {
    case Some(v) => notBlank(name, v).map(_ => value)
    case None => NotBlank(name).invalidNel
  }
}

trait ValidateMinLength {
  protected def minLength(name: String, value: String, min: Int): ValidationResult[String] =
    if (value.trim.length < min) MinLength(name, min).invalidNel else value.validNel

  protected def minLength(name: String, value: Option[String], min: Int): ValidationResult[Option[String]] = value match {
    case Some(v) => minLength(name, v, min).map(_ => value)
    case None => MinLength(name, min).invalidNel
  }
}

trait ValidateMaxLength {
  protected def maxLength(name: String, value: String, max: Int): ValidationResult[String] =
    if (value.trim.length > max) MaxLength(name, max).invalidNel else value.validNel

  protected def maxLength(name: String, value: Option[String], max: Int): ValidationResult[Option[String]] = value match {
    case Some(v) => maxLength(name, v, max).map(_ => value)
    case None => MaxLength(name, max).invalidNel
  }
}

trait Validate
  extends ValidateRequired
    with ValidateUnexpected
    with ValidateNotBlank
    with ValidateMinLength
    with ValidateMaxLength