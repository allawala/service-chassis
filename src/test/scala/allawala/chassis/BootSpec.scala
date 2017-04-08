package allawala.chassis

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import allawala.chassis.service.BootStub
import allawala.chassis.service.model.{Ping, Pong}
import com.typesafe.scalalogging.StrictLogging

class BootSpec extends RoutesSpec with StrictLogging {
  val route = Route.seal( (new BootStub).getRoute )

  "text ping" should {
    "return text pong" in {
      Get("/text/ping") ~> route ~> check {
        logger.info("Text ping")
        status should ===(StatusCodes.OK)
        responseAs[String] should ===("pong")
      }
    }
  }

  "json ping" should {
    "return json pong" in {
      import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport._
      import io.circe.generic.auto._

      Post("/json/ping", Ping("hi")) ~> acceptJsonHeader ~> route ~> check {
        logger.info("JSON ping")
        status should ===(StatusCodes.OK)
        responseAs[Pong] should ===(Pong("hi"))
      }
    }
  }
}
