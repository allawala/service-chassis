package allawala.chassis.http.route

import java.util.UUID

import akka.http.scaladsl.server.Directive1
import org.slf4j.MDC

trait RouteWrapper extends RouteSupport {

  val correlationHeader: Directive1[String] =
    optionalHeaderValueByName("X-CORRELATION-ID") map { optId =>
      val id = optId.getOrElse(UUID.randomUUID().toString)
      MDC.put("X-CORRELATION-ID", id)
      id
    }

}
