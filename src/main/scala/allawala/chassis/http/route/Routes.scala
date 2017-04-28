package allawala.chassis.http.route

import javax.inject.{Inject, Provider}

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Route

class Routes @Inject()(val routeRegistryProvider : Provider[RouteRegistry]) extends RouteWrapper {
  private val baseRoute = get {
      pathSingleSlash {
        complete { NotFound }
      }
  }

  // IMPORTANT: The logRequestResult will log request/response entities as well, which may contain sensitive information
  // TODO see if sensitive info for req/resp can be hidden via logger patterns or a loggable vs non loggable route is needed
  lazy val route: Route = handleExceptions(myExceptionHandler) {
    correlationHeader { correlationId =>
      logRequestResult(correlationId) {
        routeRegistryProvider.get().get().map(_.route).fold[Route](baseRoute){ (z, i) =>
          z ~ i
        }
      }
    }
  }
}
