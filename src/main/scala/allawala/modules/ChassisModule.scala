package allawala.modules

import javax.inject.Inject

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.google.inject.{AbstractModule, Module, Provides, Singleton}
import net.codingwell.scalaguice.ScalaModule

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class AkkaHttp @Inject()(
                          implicit val actorSystem: ActorSystem,
                          implicit val actorMaterializer: ActorMaterializer,
                          implicit val ec: ExecutionContext,
                          val logger: LoggingAdapter) {

  def run(route: Route): Unit = {
    implicit val timeout = Timeout(10.seconds)
    Http().bindAndHandle(route, "127.0.0.1", 8080).onComplete {

      case Success(b) => {
        logger.debug(s"**** AKKA HTTP SYSTEM INITIALIZED @ ${b.localAddress.getHostString}:${b.localAddress.getPort} ****.")
        sys.addShutdownHook {
          logger.debug("**** AKKA HTTP SYSTEM SHUTTING DOWN ****.")
          b.unbind().onComplete(_ => actorSystem.terminate())
        }
      }
      case Failure(e) =>
        logger.error(e, "Cannot start server", e)
        actorSystem.terminate()
    }

  }
}

class ChassisModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    bind[AkkaHttp].asEagerSingleton()
  }
}

object ChassisModule {
  def apply(): Module = new ChassisModule

  @Provides
  @Singleton
  def getAkkaMaterializer(implicit actorSystem: ActorSystem): ActorMaterializer = {
    ActorMaterializer()
  }

  @Provides
  @Singleton
  def getActorSystem(): ActorSystem = {
    ActorSystem("service-chassis")
  }

  @Provides
  @Singleton
  def getDefaultExecutionContextExecutor(implicit actorSystem: ActorSystem) : ExecutionContext = {
    actorSystem.dispatcher
  }

  @Provides
  @Singleton
  def getLoggingAdapter(implicit actorSystem: ActorSystem) : LoggingAdapter = {
    Logging(actorSystem, getClass)
  }
}
