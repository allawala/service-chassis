package allawala.chassis.util

import com.google.common.base.{CaseFormat, Converter}

object StringConverters {
  val upperCamelToLowerHyphen: Converter[String, String] = CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_HYPHEN)
}