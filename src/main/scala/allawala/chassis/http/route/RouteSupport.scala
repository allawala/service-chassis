package allawala.chassis.http.route

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.server.{Directives, Route}
import allawala.chassis.core.exception.DomainException
import allawala.{ResponseE, ResponseFE}
import cats.data.EitherT
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport
import org.slf4j.MDC

import scala.concurrent.Future

trait RouteSupport
  extends Directives
    with ErrorAccumulatingCirceSupport
    with RouteLogging {

  val XCorrelationId = "X-CORRELATION-ID"

  def onRedirectEither(resource: ResponseFE[String]): Route =
    onSuccess(resource) {
      case Left(e) => fail(e)
      case Right(url) => redirect(url, StatusCodes.TemporaryRedirect)
    }

  def onCompleteEither[T: ToEntityMarshaller](resource: ResponseFE[T]): Route =
    onCompleteEither(OK)(resource)

  // We do not import the circe auto gen for the success case as it is possible that the caller might provide a custom serializer
  def onCompleteEither[T: ToEntityMarshaller](statusCode: StatusCode)(resource: ResponseFE[T]): Route =
    onSuccess(resource) {
      case Left(e) => fail(e)
      case Right(t) => complete(statusCode -> t)
    }

  def onCompleteEitherT[T: ToEntityMarshaller](resource: EitherT[Future, DomainException, T]): Route =
    onCompleteEitherT(OK)(resource)

  def onCompleteEitherT[T: ToEntityMarshaller](statusCode: StatusCode)(resource: EitherT[Future, DomainException, T]): Route =
    onSuccess(resource.value) {
      case Left(e) => fail(e)
      case Right(t) => complete(statusCode -> t)
    }

  def completeEither[T: ToEntityMarshaller](resource: ResponseE[T]): Route = completeEither(OK)(resource)

  // We do not import the circe auto gen for the success case as it is possible that the caller might provide a custom serializer
  def completeEither[T: ToEntityMarshaller](statusCode: StatusCode)(resource: ResponseE[T]): Route =
    resource match {
      case Left(e) => fail(e)
      case Right(t) => complete(statusCode -> t)
    }

  def fail(ex: DomainException): Route = {
    fail(ex.statusCode, ex)
  }

  def fail(statusCode: StatusCode, ex: DomainException): Route = {
    import io.circe.generic.auto._
    extractRequestContext { requestContext =>
      logError(requestContext.request, ex)
      complete(statusCode -> toErrorEnvelope(requestContext.request, MDC.get(XCorrelationId), ex))
    }
  }
}
