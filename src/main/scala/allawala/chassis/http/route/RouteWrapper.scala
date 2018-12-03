package allawala.chassis.http.route

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server._
import allawala.chassis.auth.exception.AuthenticationException
import allawala.chassis.core.exception.{DomainException, UnexpectedException, ValidationException}
import allawala.chassis.core.rejection.DomainRejection
import allawala.chassis.core.validation.RequiredField
import cats.data.NonEmptyList
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport
import org.apache.shiro.authc.{AuthenticationException => ShiroAuthenticationException}
import org.slf4j.MDC

trait RouteWrapper extends RouteSupport {

  def routesExceptionHandler: ExceptionHandler = {
    ExceptionHandler {
      // This can happen on a Future.failed { with some domain exception}
      case e: DomainException => fail(e)
      case e: IllegalArgumentException => fail(BadRequest, UnexpectedException(errorCode = "invalid.request", e))
      case e: ShiroAuthenticationException => fail(AuthenticationException(cause = e))
      case e: NoSuchElementException =>
        // For akka http cors. Ignore logging
        complete(NotFound -> e.getMessage)
      case e: Exception => fail(InternalServerError, UnexpectedException(cause = e))
    }
  }

  def routesRejectionHandler: RejectionHandler = RejectionHandler.newBuilder().handle {
    case rejection: DomainRejection ⇒ fail(rejection.ex)
  }.result()

  /*
    When decoding an incoming request, if a required field is missing from the payload, it results in the generic request content
    was malformed response. This is due to circe throwing a io.circe.Errors exception. In this case, we want to try and see if
    we can turn this into a validation exception so that it can be handled better on the client side.
  */
  def circeRejectHandler: RejectionHandler = RejectionHandler.newBuilder().handle {
    case MalformedRequestContentRejection(msg, ex) if ex.isInstanceOf[ErrorAccumulatingCirceSupport.DecodingFailures] ⇒
      val regex = "DownField\\((.*?)\\)".r
      val errorMessages = ex.asInstanceOf[ErrorAccumulatingCirceSupport.DecodingFailures].failures.map(_.getMessage).toList
      val matches = errorMessages.map(e => regex.findAllMatchIn(e).map(_.group(1)).toList)
      val requiredFields = for (m <- matches) yield {
        val field = m.reverse.mkString(".")
        RequiredField(field)
      }
      fail(ValidationException(NonEmptyList.fromListUnsafe(requiredFields)))
  }.result()

  def correlationHeader: Directive1[String] =
    optionalHeaderValueByName(XCorrelationId) map { optId =>
      val id = optId.getOrElse(UUID.randomUUID().toString)
      MDC.put(XCorrelationId, id)
      id
    }

}