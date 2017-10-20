package allawala.chassis.http.route

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.server.{Directives, Route}
import allawala.chassis.core.exception.DomainException
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport
import org.slf4j.MDC

import scala.concurrent.Future

trait RouteSupport
  extends Directives
    with ErrorAccumulatingCirceSupport
    with RouteLogging {

  val XCorrelationId = "X-CORRELATION-ID"

  def onCompleteEither[T: ToEntityMarshaller](resource: Future[Either[DomainException, T]]): Route =
    onCompleteEither(OK)(resource)

  def onCompleteEither[T: ToEntityMarshaller](statusCode: StatusCode)
                                             (resource: Future[Either[DomainException, T]]): Route =
    extractRequestContext { requestContext =>
      onSuccess(resource) {
        case Left(e) =>
          import io.circe.generic.auto._
          logError(requestContext.request, e)
          complete(e.statusCode -> e.toErrorEnvelope(MDC.get(XCorrelationId)))
        case Right(t) =>
          // We do not import the circe auto gen here as it is possible that the caller might provide a custom serializer
          complete(statusCode -> t)
      }
    }

  def completeEither[T: ToEntityMarshaller](resource: Either[DomainException, T]): Route = completeEither(OK)(resource)

  def completeEither[T: ToEntityMarshaller](statusCode: StatusCode)(resource: Either[DomainException, T]): Route =
    extractRequestContext { requestContext =>
      resource match {
        case Left(e) =>
          import io.circe.generic.auto._
          logError(requestContext.request, e)
          complete(e.statusCode -> e.toErrorEnvelope(MDC.get(XCorrelationId)))
        case Right(t) =>
          // We do not import the circe auto gen here as it is possible that the caller might provide a custom serializer
          complete(statusCode -> t)
      }
    }
}
