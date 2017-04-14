package allawala.chassis.health

import com.google.common.base.{CaseFormat, Converter}

trait HealthCheckSupport extends nl.grons.metrics.scala.DefaultInstrumented {
  val converter: Converter[String, String] = CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_HYPHEN)

  lazy protected val checkName: String = converter.convert(getClass.getSimpleName)

  protected def toCheckName(checkResultKey: String): String = checkResultKey.substring(checkResultKey.lastIndexOf(".") + 1)
}
