package allawala.chassis.service

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server._
import allawala.chassis.service.model.{Ping, Pong}
import com.google.inject.Module
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport

class BootStub extends Boot with Directives with ErrorAccumulatingCirceSupport {
  override def getModules: List[Module] = List[Module]()

  override def getRoute: Route = {
    import io.circe.generic.auto._
    get {
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
      }
  }
}
