package allawala.chassis.http.route

import javax.inject.{Inject, Provider}

import akka.http.scaladsl.server.Route

class Routes @Inject()(
                        val securityManager: SecurityManager,
                        val routeRegistryProvider: Provider[RouteRegistry]
                      ) extends RouteWrapper {

  // IMPORTANT: The logRequestResult will log request/response entities as well, which may contain sensitive information
  // TODO see if sensitive info for req/resp can be hidden via logger patterns or a loggable vs non loggable route is needed
  lazy val route: Route = handleExceptions(myExceptionHandler) {
    correlationHeader { correlationId =>
      logRequestResult(correlationId) {
        routeRegistryProvider.get().get().map(_.route).reduce[Route] { (z, i) =>
          z ~ i
        }
      }
    }
  }
}
