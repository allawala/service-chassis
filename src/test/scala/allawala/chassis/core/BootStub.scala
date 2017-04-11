package allawala.chassis.core

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server._
import allawala.chassis.core.model.{Ping, Pong}
import allawala.chassis.http.route.RouteWrapper
import com.google.inject.Module
import com.google.inject.name.Names
import net.codingwell.scalaguice.InjectorExtensions._

import scala.concurrent.{ExecutionContext, Future}

class BootStub extends Boot with RouteWrapper {
  override def getModules: List[Module] = List[Module]()

  lazy val system = injector.instance[ActorSystem]
  lazy val asyncService = new AsyncService(system)

  override lazy val getRoute: Route = {
    import io.circe.generic.auto._
    correlationHeader { cid =>
      get {
        println(s"***** CID = $cid")
        path("text" / "ping") {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "pong"))
        }
      } ~
        post {
          path("json" / "ping") {
            entity(as[Ping]) { p =>
              complete(Pong(p.name))
            }
          }
        } ~
        get {
          path("async") {
            parameter("name") { name =>
              logger.info(s"Handling $name")
              onComplete(asyncService.doAsync(name)) { result =>
                logger.info(s"Completing future with $result for $name")
                complete(result)
              }
            }
          } ~
            path("async1") {
              implicit lazy val executionContext = injector.instance[ExecutionContext](Names.named("default-dispatcher"))
              onComplete(Future {
                logger.info(s"aysnc1")
                "async1 "
              }) { result =>
                logger.info(s"Completing future with $result")
                complete(result)
              }
            }
        }

    }
  }
}
