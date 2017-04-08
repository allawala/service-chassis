package allawala.chassis.http.model

import javax.inject.{Inject, Named}

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import allawala.chassis.config.model.Configuration
import allawala.chassis.http.route.Routes

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class AkkaHttp @Inject()(
                          val config: Configuration,
                          val routes: Routes,
                          val logger: LoggingAdapter
                        )(
                          implicit val actorSystem: ActorSystem,
                          implicit val actorMaterializer: ActorMaterializer,
                          @Named("default-dispatcher") implicit val ec: ExecutionContext
                        ) {

  def run(): Unit = {
    implicit val timeout = Timeout(10.seconds)
    Http().bindAndHandle(routes.route, config.httpConfig.host, config.httpConfig.port).onComplete {

      case Success(b) => {
        logger.info(s"**** [${actorSystem.name}] INITIALIZED @ ${b.localAddress.getHostString}:${b.localAddress.getPort} ****.")
        sys.addShutdownHook {
          logger.info(s"**** [${actorSystem.name}] SHUTTING DOWN ****.")
          b.unbind().onComplete(_ => actorSystem.terminate())
        }
      }
      case Failure(e) =>
        logger.error(e, s"**** [${actorSystem.name}] FAILED TO START **** ")
        actorSystem.terminate()
    }

  }
}
