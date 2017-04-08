package allawala.chassis.http.route

import javax.inject.{Inject, Provider}

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Route
import allawala.chassis.http.model.RouteRegistry

class Routes @Inject()(val routeRegistryProvider : Provider[RouteRegistry]) extends RouteWrapper {
  private val baseRoute = get {
      pathSingleSlash {
        complete { NotFound }
      }
  }

  lazy val route: Route = correlationHeader { correlationId =>
    routeRegistryProvider.get().getRoutes().fold[Route](baseRoute){ (z, i) =>
      z ~ i
    }
  }
}
