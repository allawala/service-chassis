package allawala.chassis.http.route
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Route

class PingRoute extends HasRoute with RouteSupport {
  override def route: Route = get {
    path("ping") {
      logger.info("ping -> pong")
      complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "pong"))
    }
  }
}
