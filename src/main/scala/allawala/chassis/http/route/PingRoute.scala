package allawala.chassis.http.route
import jakarta.inject.Inject

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Route
import allawala.chassis.i18n.service.I18nService

class PingRoute @Inject()(override val i18nService: I18nService) extends HasRoute with RouteSupport {
  override def route: Route = get {
    path("ping") {
      logger.info("ping -> pong")
      complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "pong"))
    } ~ path("i18n") {
      extractRequestContext { requestContext =>
        val message = i18nService.getForRequest(requestContext.request, "authorization.error")
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, message))
      }
    }
  }
}
