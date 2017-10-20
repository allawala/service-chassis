package allawala.chassis.http.route

import javax.inject.{Inject, Provider}

import akka.http.scaladsl.server.{Directive, RejectionHandler, Route}
import allawala.chassis.config.model.CorsConfig
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._

class Routes @Inject()(
                        val securityManager: SecurityManager,
                        val routeRegistryProvider: Provider[RouteRegistry],
                        val corsConfig: CorsConfig
                      ) extends RouteWrapper with CorsSupport {

  override val allowedOrigins: Seq[String] = corsConfig.allowedOrigins
  lazy val rejectionHandler: RejectionHandler = corsRejectionHandler
    .withFallback(optionsRejectionHandler)
    .withFallback(routesRejectionHandler)
    .withFallback(RejectionHandler.default)

  lazy val handleErrors: Directive[Unit] = handleRejections(rejectionHandler) & handleExceptions(routesExceptionHandler)

  // IMPORTANT: The logRequestResult will log request/response entities as well, which may contain sensitive information
  // TODO see if sensitive info for req/resp can be hidden via logger patterns or a loggable vs non loggable route is needed
  // See https://github.com/lomigmegard/akka-http-cors
  lazy val route: Route = handleErrors {
    cors(corsSettings) {
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
