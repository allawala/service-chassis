package allawala.chassis.core.validation

trait ValidationError {
  def field: String
  def code: String
  def parameters: Seq[AnyRef] = Seq.empty
}

final case class RequiredField(override val field: String) extends ValidationError {
  override val code = "validation.error.required"
}

final case class UnexpectedField(override val field: String) extends ValidationError {
  override val code = "validation.error.unexpected"
}

final case class NotBlank(override val field: String) extends ValidationError {
  override val code = "validation.error.blank"
}

final case class MinLength(override val field: String, min: Int) extends ValidationError {
  override val code = "validation.error.length.min"
  override def parameters: Seq[AnyRef] = Seq(Int.box(min))
}

final case class MaxLength(override val field: String, max: Int) extends ValidationError {
  override val code = "validation.error.length.max"
  override def parameters: Seq[AnyRef] = Seq(Int.box(max))
}

final case class Length(override val field: String, min: Int, max: Int) extends ValidationError {
  override val code = "validation.error.length"
  override def parameters: Seq[AnyRef] = Seq(Int.box(min), Int.box(max))
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
