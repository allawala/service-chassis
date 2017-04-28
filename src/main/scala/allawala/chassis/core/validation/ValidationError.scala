package allawala.chassis.core.validation

/*
  Eg
  case class Child(age: Int)
  case class Parent(name: String, child: Child)

  The format for the code is as follows
  validation.error.${field}.${errorType}

  where ${field} might be "name" or "child.age"
  where ${errorType} might be "required" or "length.min" etc.

  If the error requires some expected value to be passed back
  validation.error.${field}.${errorType}:${value}

  If the error requires some expected value range to be passed back
  validation.error.${field}.${errorType}:${[min, max]}

  The client receiving this payload can parse the code to determine the key for any I18N messages if it so chooses.
  The client may also simply parse the field name from the code and display the default message for the fields in error
 */

trait ValidationError {
  def code: String
  def message: String
}

final case class RequiredField(field: String) extends ValidationError {
  override val code = s"validation.error.$field.required"
  override val message = s"$field is required"
}

final case class UnexpectedField(field: String) extends ValidationError {
  override val code = s"validation.error.$field.unexpected"
  override val message = s"$field should not be specified"
}

final case class NotBlank(field: String) extends ValidationError {
  override val code = s"validation.error.$field.blank"
  override val message = s"$field must not be blank"
}

final case class MinLength(field: String, min: Int) extends ValidationError {
  override val code = s"validation.error.$field.length.min:$min"
  override val message: String = s"$field length should be least at least $min characters"
}

final case class MaxLength(field: String, max: Int) extends ValidationError {
  override val code = s"validation.error.$field.length.max:$max"
  override val message: String = s"$field length should be at most $max characters"
}

final case class Length(field: String, min: Int, max: Int) extends ValidationError {
  override val code = s"validation.error.$field.length:[$min,$max]"
  override val message: String = s"$field length should be between $min and $max characters"
}

// TODO should be generic T instead of Int
//final case class MinValue(field: String, min: Int) extends ValidationError {
//  override val code = s"validation.error.$field.value.min:$min"
//  override val message: String = s"$field value should be at least $min"
//}
//
//final case class MaxValue(field: String, max: Int) extends ValidationError {
//  override val code = s"validation.error.$field.value.max:$max"
//  override val message: String = s"$field value should be at most $max"
//}
//
//final case class Value(field: String, min: Int, max: Int) extends ValidationError {
//  override val code = s"validation.error.$field.value:[$min,$max]"
//  override val message: String = s"$field value should be between $min and $max"
//}
