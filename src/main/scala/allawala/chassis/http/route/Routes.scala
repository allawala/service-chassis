package allawala.chassis.http.route

import akka.http.scaladsl.server.{Directive, RejectionHandler, Route}
import allawala.chassis.i18n.service.I18nService

import jakarta.inject.{Inject, Provider}

class Routes @Inject()(
                        val routeRegistryProvider: Provider[RouteRegistry],
                        override val i18nService: I18nService
                      ) extends RouteWrapper {

  lazy val rejectionHandler: RejectionHandler = routesRejectionHandler
    .withFallback(circeRejectHandler)
    .withFallback(RejectionHandler.default)

  lazy val handleErrors: Directive[Unit] = handleRejections(rejectionHandler) & handleExceptions(routesExceptionHandler)

  // IMPORTANT: The logRequestResult will log request/response entities as well, which may contain sensitive information
  // TODO see if sensitive info for req/resp can be hidden via logger patterns or a loggable vs non loggable route is needed
  // See https://github.com/lomigmegard/akka-http-cors
  lazy val route: Route = handleErrors {
    cors() {
      handleErrors {
        correlationHeader { correlationId =>
          logRequestResult(correlationId) {
            routeRegistryProvider.get().get().map(_.route).reduce[Route] { (z, i) =>
              z ~ i
            }
          }
        }
      }
    }
  }
}
