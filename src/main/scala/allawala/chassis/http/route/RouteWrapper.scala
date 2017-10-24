package allawala.chassis.http.route

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server._
import allawala.chassis.auth.exception.AuthenticationException
import allawala.chassis.core.exception.{DomainException, UnexpectedException}
import allawala.chassis.core.rejection.DomainRejection
import org.apache.shiro.authc.{AuthenticationException => ShiroAuthenticationException}
import org.slf4j.MDC

trait RouteWrapper extends RouteSupport {

  def routesExceptionHandler: ExceptionHandler = {
    import io.circe.generic.auto._

    ExceptionHandler {
      // This can happen on a Future.failed { with some domain exception}
      case e: DomainException => fail(e)
      case e: IllegalArgumentException => fail(BadRequest, UnexpectedException(errorCode = "invalid.request", e))
      case e: ShiroAuthenticationException =>
        extractRequest { request =>
          val ae = AuthenticationException(message = e.getMessage, cause = e)
          logError(request, ae)
          // We do not want to expose the actual reason behind the authenticaion failure, we just need to log it
          complete(ae.statusCode -> ae.copy(message = "authentication failure").toErrorEnvelope(MDC.get(XCorrelationId)))
        }
      case e: NoSuchElementException =>
        // For akka http cors. Ignore logging
        complete(NotFound -> e.getMessage)
      case e: Exception => fail(InternalServerError, UnexpectedException(cause = e))
    }
  }

  def routesRejectionHandler: RejectionHandler = RejectionHandler.newBuilder().handle {
    case rejection: DomainRejection ⇒ fail(rejection.ex)
  }.result()

  val correlationHeader: Directive1[String] =
    optionalHeaderValueByName(XCorrelationId) map { optId =>
      val id = optId.getOrElse(UUID.randomUUID().toString)
      MDC.put(XCorrelationId, id)
      id
    }

}