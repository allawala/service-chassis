package allawala.chassis.http.service

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.Materializer
import allawala.chassis.config.model.{BaseConfig, Environment}
import allawala.chassis.core.exception.InitializationException
import allawala.chassis.core.util.LogWrapper
import allawala.chassis.http.lifecycle.LifecycleAwareRegistry
import allawala.chassis.http.route.Routes
import allawala.chassis.i18n.service.I18nService
import ch.qos.logback.core.util.StatusPrinter2

import jakarta.inject.{Inject, Named, Provider}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

class AkkaHttpService @Inject()(
                                 val baseConfig: BaseConfig,
                                 val routes: Routes,
                                 val environment: Environment,
                                 val lifecycleAwareRegistryProvider: Provider[LifecycleAwareRegistry],
                                 override val i18nService: I18nService
                               )(
                                 implicit val actorSystem: ActorSystem,
                                 implicit val actorMaterializer: Materializer,
                                 @Named("default-dispatcher") implicit val ec: ExecutionContext
                               ) extends LogWrapper {

  def run(): Unit = {
    printLogbackConfig()
    executeLifeCycleEventsAndThen("PRE START", Future.sequence(lifecycleAwareRegistryProvider.get().get().map(_.preStart()))) {
      bind()
    } {
      // Try and clean up
      preStop()
    }
  }

  private def bind() = {
    Http().newServerAt(baseConfig.httpConfig.host, baseConfig.httpConfig.port).bindFlow(routes.route).onComplete {
      case Success(b) =>
        logger.info(s"**** [${environment.entryName}] [${actorSystem.name}] INITIALIZED @ ${b.localAddress.getHostString}:${b.localAddress.getPort} ****.")
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
      case Failure(e) =>
        logger.error(s"**** [${environment.entryName}] [${actorSystem.name}] FAILED TO START **** ", e)
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

  private def printLogbackConfig(): Unit = {
    import ch.qos.logback.classic.LoggerContext
    import org.slf4j.LoggerFactory

    val context: LoggerContext = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    new StatusPrinter2().print(context)
  }

  private def executeLifeCycleEventsAndThen(name: String, events: Future[Seq[Either[InitializationException, Unit]]])
                                           (onSuccess: => Unit)
                                           (onFailure: => Unit): Unit = {
    events.onComplete {
      case Success(results) =>
        val failed: Seq[Either[InitializationException, Unit]] = results.filter(_.isLeft)
        if (failed.isEmpty) {
          logger.info(s"**** [${environment.entryName}] [${actorSystem.name}] $name SUCCESS ****")
          onSuccess
        } else {
          failed.map(_.swap.toOption.get).foreach(ex => logIt(ex))
          logger.error(s"**** [${environment.entryName}] [${actorSystem.name}] $name FAILURE ****")
          onFailure
        }
      case Failure(e) =>
        logger.error(s"**** [${environment.entryName}] [${actorSystem.name}] $name FAILURE **** ", e)
        onFailure
    }
  }
}
