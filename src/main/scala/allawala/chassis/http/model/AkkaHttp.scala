package allawala.chassis.http.model

import javax.inject.Inject

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.Http
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import allawala.chassis.config.model.Configuration

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class AkkaHttp @Inject()(
                          val config: Configuration,
                          implicit val actorSystem: ActorSystem,
                          implicit val actorMaterializer: ActorMaterializer,
                          implicit val ec: ExecutionContext,
                          val logger: LoggingAdapter) {

  def run(route: Route): Unit = {
    implicit val timeout = Timeout(10.seconds)
    Http().bindAndHandle(route, config.httpConfig.host, config.httpConfig.port).onComplete {

      case Success(b) => {
        logger.debug(
          s"**** [${actorSystem.name}] INITIALIZED @ ${b.localAddress.getHostString}:${b.localAddress.getPort} ****."
        )
        sys.addShutdownHook {
          logger.debug(s"**** [${actorSystem.name}] SHUTTING DOWN ****.")
          b.unbind().onComplete(_ => actorSystem.terminate())
        }
      }
      case Failure(e) =>
        logger.error(e, s"**** [${actorSystem.name}] FAILED TO START **** ")
        actorSystem.terminate()
    }

  }
}
