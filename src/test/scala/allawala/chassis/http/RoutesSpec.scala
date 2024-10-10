package allawala.chassis.http

import akka.http.scaladsl.model.MediaRange
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

trait RoutesSpec extends AnyWordSpecLike with Matchers with ScalatestRouteTest  {
  val acceptJsonHeader: RequestTransformer = addHeader(akka.http.scaladsl.model.headers.Accept(MediaRange(`application/json`)))
}
