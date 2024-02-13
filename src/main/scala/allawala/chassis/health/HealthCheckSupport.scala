package allawala.chassis.health

import allawala.chassis.util.StringConverters
import nl.grons.metrics4.scala.DefaultInstrumented

trait HealthCheckSupport extends DefaultInstrumented {
  lazy protected val checkName: String = StringConverters.upperCamelToLowerHyphen.convert(getClass.getSimpleName)

  protected def toCheckName(checkResultKey: String): String = checkResultKey.substring(checkResultKey.lastIndexOf(".") + 1)
}
