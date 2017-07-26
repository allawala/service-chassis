package allawala.chassis.health

import allawala.chassis.util.StringConverters

trait HealthCheckSupport extends nl.grons.metrics.scala.DefaultInstrumented {
  lazy protected val checkName: String = StringConverters.upperCamelToLowerHyphen.convert(getClass.getSimpleName)

  protected def toCheckName(checkResultKey: String): String = checkResultKey.substring(checkResultKey.lastIndexOf(".") + 1)
}
