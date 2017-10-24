package allawala.chassis.common

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._
import scala.language.postfixOps

trait FutureSpec {
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  val timeout: FiniteDuration = 50000 millis
}
