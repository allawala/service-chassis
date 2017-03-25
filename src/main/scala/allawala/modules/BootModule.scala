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

class BootModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    install(new ConfigModule)

    bind[AkkaHttp].asEagerSingleton()
  }
}

object BootModule {
  def apply(): Module = new BootModule

  @Provides
  @Singleton
  def getAkkaMaterializer(implicit actorSystem: ActorSystem): ActorMaterializer = {
    ActorMaterializer()
  }

  @Provides
  @Singleton
  def getActorSystem(config: Configuration): ActorSystem = {
    ActorSystem(config.name)
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
