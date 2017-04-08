package allawala.chassis.core

import akka.http.scaladsl.model.MediaRange
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest._

trait RoutesSpec extends WordSpecLike with Matchers with ScalatestRouteTest  {
  val acceptJsonHeader = addHeader(akka.http.scaladsl.model.headers.Accept(MediaRange(`application/json`)))
}
