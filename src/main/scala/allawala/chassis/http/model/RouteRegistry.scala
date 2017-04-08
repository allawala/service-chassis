package allawala.chassis.http.model

import akka.http.scaladsl.server.Route
import allawala.chassis.http.route.HasRoute

import scala.collection.concurrent.TrieMap

class RouteRegistry {
  private val registry = TrieMap[String, Route]()

  def register(name: String, hasRoute: HasRoute): Unit = {
    registry += (name -> hasRoute.route)
  }

  def getRoutes(): Seq[Route] = {
    registry.values.toSeq
  }
}
