package allawala.chassis.http.route

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server._
import allawala.chassis.auth.exception.AuthenticationException
import allawala.chassis.core.exception.{DomainException, UnexpectedException}
import org.apache.shiro.authc.{AuthenticationException => ShiroAuthenticationException}
import org.slf4j.MDC

trait RouteWrapper extends RouteSupport {

  def myExceptionHandler: ExceptionHandler = {
    import io.circe.generic.auto._

    ExceptionHandler {
      // This can happen on a Future.failed { with some domain exception}
      case e: DomainException =>
        extractRequest { request =>
          logError(request, e)
          complete(e.statusCode -> e.toErrorEnvelope(MDC.get(XCorrelationId)))
        }
      case e: IllegalArgumentException =>
        extractRequest { request =>
          val ue = UnexpectedException(errorCode = "invalid.request", e)
          logError(request, ue)
          complete(BadRequest -> ue.toErrorEnvelope(MDC.get(XCorrelationId)))
        }
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
      case e: Exception =>
        extractRequest { request =>
          val ue = UnexpectedException(cause = e)
          logError(request, ue)
          complete(InternalServerError -> ue.toErrorEnvelope(MDC.get(XCorrelationId)))
        }
    }
  }

  val correlationHeader: Directive1[String] =
    optionalHeaderValueByName(XCorrelationId) map { optId =>
      val id = optId.getOrElse(UUID.randomUUID().toString)
      MDC.put(XCorrelationId, id)
      id
    }

}