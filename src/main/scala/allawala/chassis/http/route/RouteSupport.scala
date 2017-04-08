package allawala.chassis.http.route

import akka.http.scaladsl.server.Directives
import com.typesafe.scalalogging.StrictLogging
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport

trait RouteSupport
  extends Directives
  with ErrorAccumulatingCirceSupport
  with StrictLogging
