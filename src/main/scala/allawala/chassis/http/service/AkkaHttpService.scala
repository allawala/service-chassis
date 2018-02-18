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

import scala.concurrent.{Await, ExecutionContext, Future}
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
    executeLifeCycleEventsAndThen("PRE START", Future.sequence(lifecycleAwareRegistryProvider.get().get().map(_.preStart()))) {
      bind()
    } {
      // Try and clean up
      preStop()
    }
  }

  private def bind() = {
    Http().bindAndHandle(routes.route, baseConfig.httpConfig.host, baseConfig.httpConfig.port).onComplete {

      case Success(b) => {
        logger.info(s"**** [${environment.entryName}] [${actorSystem.name}] INITIALIZED @ ${b.localAddress.getHostString}:${b.localAddress.getPort} ****.")
        printLogbackConfig()

        executeLifeCycleEventsAndThen("POST START", Future.sequence(lifecycleAwareRegistryProvider.get().get().map(_.postStart()))) {
          () // If successful, do nothing
        } {
          // Try and clean up
          preStop(Some(b))
        }

        sys.addShutdownHook {
          logger.info(s"**** [${environment.entryName}] [${actorSystem.name}] SHUTTING DOWN ****.")
          preStop(Some(b))
        }
      }
      case Failure(e) =>
        logger.error(e, s"**** [${environment.entryName}] [${actorSystem.name}] FAILED TO START **** ")
        actorSystem.terminate()
    }
  }

  private def preStop(binding: Option[Http.ServerBinding] = None) = {
    def unbindAndTerminate(binding: Option[Http.ServerBinding]) = {
      binding match {
        case Some(b) => b.unbind().onComplete(_ => actorSystem.terminate())
        case None => actorSystem.terminate()
      }
    }

    executeLifeCycleEventsAndThen("PRE STOP", Future.sequence(lifecycleAwareRegistryProvider.get().get().map(_.preStop()))) {
      unbindAndTerminate(binding)
    } {
      unbindAndTerminate(binding)
    }

    Await.result(actorSystem.whenTerminated, baseConfig.awaitTermination)
  }

  private def printLogbackConfig() = {
    import ch.qos.logback.classic.LoggerContext
    import ch.qos.logback.core.util.StatusPrinter
    import org.slf4j.LoggerFactory

    val context: LoggerContext = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    StatusPrinter.print(context)
  }

  private def executeLifeCycleEventsAndThen(name: String, events: Future[Seq[Either[InitializationException, Unit]]])
                                           (onSuccess: => Unit)
                                           (onFailure: => Unit) = {
    events.onComplete {
      case Success(results) =>
        val failed: Seq[Either[InitializationException, Unit]] = results.filter(_.isLeft)
        if (failed.isEmpty) {
          logger.error(s"**** [${environment.entryName}] [${actorSystem.name}] $name SUCCESS ****")
          onSuccess
        } else {
          failed.map(_.left).map(_.get).foreach(ex => logger.error(ex, ex.getMessage))
          logger.error(s"**** [${environment.entryName}] [${actorSystem.name}] $name FAILURE ****")
          onFailure
        }
      case Failure(e) =>
        logger.error(e, s"**** [${environment.entryName}] [${actorSystem.name}] $name FAILURE **** ")
        onFailure
    }
  }
}
