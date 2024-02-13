package allawala.chassis.http.route

import akka.http.scaladsl.model.HttpRequest
import allawala.chassis.core.exception.{DomainException, ValidationException}
import allawala.chassis.core.model.{ErrorEnvelope, HttpErrorLog, ValidationEnvelope}
import allawala.chassis.i18n.service.I18nService
import com.typesafe.scalalogging.StrictLogging

trait RouteLogging extends StrictLogging {
  def i18nService: I18nService

  def logError(request: HttpRequest, e: DomainException): Unit = {
    import io.circe.generic.auto._
    import io.circe.syntax._

    val log = HttpErrorLog(
      method = request.method.value,
      uri = request.uri.toString(),
      errorType = e.errorType,
      errorCode = e.errorCode,
      errorMessage = i18nService.getForDefaultLocale(e.errorCode, e.messageParameters), // logging is always be in english
      thread = e.thread,
      payload = e.logMap.view.mapValues(_.toString).toMap,
      validationPayload = getErrorPayload(request, e)
    )

    logger.error(log.asJson.noSpaces, e)
    Option(e.cause).foreach(logger.error("caused by", _))
  }

  def toErrorEnvelope(request: HttpRequest, correlationId: String, e: DomainException): ErrorEnvelope = ErrorEnvelope(
    errorType = e.errorType,
    correlationId = correlationId,
    errorCode = e.errorCode,
    errorMessage = i18nService.getForRequest(request, e.errorCode, e.messageParameters),
    details = getErrorPayload(request, e)
  )

  private def getErrorPayload(request: HttpRequest, e: DomainException) = {
    e match {
      case ve: ValidationException =>
        ve.validationErrors.toList.groupBy(_.field).view.mapValues { grouped =>
          grouped map { g =>
            val message = i18nService.getForRequest(request, g.code, g.parameters)
            ValidationEnvelope(g.code, message)
          }
        }.toMap
      case _ => Map.empty[String, List[ValidationEnvelope]]
    }
  }
}
