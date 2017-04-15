package allawala.chassis.http.model

import javax.inject.{Inject, Named, Provider}

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import allawala.chassis.config.model.{BaseConfig, Environment}
import allawala.chassis.http.lifecycle.LifecycleAwareRegistry
import allawala.chassis.http.route.Routes

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class AkkaHttp @Inject()(
                          val baseConfig: BaseConfig,
                          val routes: Routes,
                          val environment: Environment,
                          val logger: LoggingAdapter,
                          val lifecycleAwareRegistryProvider : Provider[LifecycleAwareRegistry]
                        )(
                          implicit val actorSystem: ActorSystem,
                          implicit val actorMaterializer: ActorMaterializer,
                          @Named("default-dispatcher") implicit val ec: ExecutionContext
                        ) {

  def run(): Unit = {
    implicit val timeout = Timeout(10.seconds)

    val started: Seq[Future[Unit]] = lifecycleAwareRegistryProvider.get().get().map(_.preStart())
    Future.sequence(started).onComplete {
      case Success(_) => bind()
      case Failure(e) =>
        logger.error(e, s"**** [${environment.entryName}] [${actorSystem.name}] PRE START FAILURE **** ")
        actorSystem.terminate()

    }
  }

  private def bind() = {
    Http().bindAndHandle(routes.route, baseConfig.httpConfig.host, baseConfig.httpConfig.port).onComplete {

      case Success(b) => {
        logger.info(s"**** [${environment.entryName}] [${actorSystem.name}] INITIALIZED @ ${b.localAddress.getHostString}:${b.localAddress.getPort} ****.")
        printLogbackConfig()
        sys.addShutdownHook {
          logger.info(s"**** [${environment.entryName}] [${actorSystem.name}] SHUTTING DOWN ****.")
          b.unbind().onComplete(_ => actorSystem.terminate())
        }
      }
      case Failure(e) =>
        logger.error(e, s"**** [${environment.entryName}] [${actorSystem.name}] FAILED TO START **** ")
        actorSystem.terminate()
    }
  }

  private def printLogbackConfig() = {
    import ch.qos.logback.classic.LoggerContext
    import ch.qos.logback.core.util.StatusPrinter
    import org.slf4j.LoggerFactory

    val context: LoggerContext = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    StatusPrinter.print(context)
  }
}
