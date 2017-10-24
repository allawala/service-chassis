package allawala.chassis.http.service

import javax.inject.{Inject, Named, Provider}

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import allawala.chassis.config.model.{BaseConfig, Environment}
import allawala.chassis.core.exception.InitializationException
import allawala.chassis.http.lifecycle.LifecycleAwareRegistry
import allawala.chassis.http.route.Routes

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class AkkaHttpService @Inject()(
                                 val baseConfig: BaseConfig,
                                 val routes: Routes,
                                 val environment: Environment,
                                 val logger: LoggingAdapter,
                                 val lifecycleAwareRegistryProvider: Provider[LifecycleAwareRegistry]
                               )(
                                 implicit val actorSystem: ActorSystem,
                                 implicit val actorMaterializer: ActorMaterializer,
                                 @Named("default-dispatcher") implicit val ec: ExecutionContext
                               ) {

  def run(): Unit = {
    val started = lifecycleAwareRegistryProvider.get().get().map(_.preStart())
    Future.sequence(started).onComplete {
      case Success(results) =>
        // Filter out all the valid results so that only the failed ones are in the sequence
        val failed: Seq[Either[InitializationException, Unit]] = results.filter(_.isLeft)
        if (failed.isEmpty) {
          bind()
        } else {
          failed.map(_.left).map(_.get).foreach(ex => logger.error(ex, ex.getMessage))
          logger.error(s"**** [${environment.entryName}] [${actorSystem.name}] PRE START FAILURE ****")
          actorSystem.terminate()
        }
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
