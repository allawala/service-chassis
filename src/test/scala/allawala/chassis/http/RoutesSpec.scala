package allawala.chassis.http

import org.apache.pekko.http.scaladsl.model.MediaRange
import org.apache.pekko.http.scaladsl.model.MediaTypes._
import org.apache.pekko.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

trait RoutesSpec extends AnyWordSpecLike with Matchers with ScalatestRouteTest  {
  val acceptJsonHeader: RequestTransformer = addHeader(org.apache.pekko.http.scaladsl.model.headers.Accept(MediaRange(`application/json`)))
}
