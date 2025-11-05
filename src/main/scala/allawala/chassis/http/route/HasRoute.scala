package allawala.chassis.http.route

import org.apache.pekko.http.scaladsl.server.Route

trait HasRoute {
  def route: Route
}
