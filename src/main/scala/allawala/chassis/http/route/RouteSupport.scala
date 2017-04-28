package allawala.chassis.http.route

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.{HttpRequest, StatusCode}
import akka.http.scaladsl.server.{Directives, Route}
import allawala.chassis.core.exception.{DomainException, HttpErrorLog}
import com.typesafe.scalalogging.StrictLogging
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport
import org.slf4j.MDC

import scala.concurrent.Future

trait RouteSupport
  extends Directives
    with ErrorAccumulatingCirceSupport
    with StrictLogging {

  val XCorrelationId = "X-CORRELATION-ID"

  protected def logError(request: HttpRequest, e: DomainException, errorCode: Option[String] = None) = {
    import io.circe.generic.auto._
    import io.circe.syntax._

    val log = HttpErrorLog(
      method = request.method.value,
      uri = request.uri.toString(),
      errorType = e.errorType,
      errorCode = e.errorCode,
      errorMessage = e.message,
      thread = e.thread,
      payload = e.errorMap ++ e.logMap.mapValues(_.toString)
    )

    logger.error(log.asJson.noSpaces, e)
  }

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
