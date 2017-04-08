package allawala.chassis.core

import akka.actor.ActorSystem
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.Future

// TODO remove this once the chassis is complete and tests are refactored
class AsyncService(val system: ActorSystem) extends StrictLogging {
  implicit val executionContext = system.dispatchers.lookup("blocking-fixed-pool-dispatcher")

  def doAsync(name: String): Future[String] = Future {
    logger.info(s"This is to test MDC propagation for $name")
    "success"
  }
}
