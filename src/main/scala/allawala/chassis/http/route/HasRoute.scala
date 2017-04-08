package allawala.chassis.http.route

import akka.http.scaladsl.server.Route

trait HasRoute {
  def route: Route
}
