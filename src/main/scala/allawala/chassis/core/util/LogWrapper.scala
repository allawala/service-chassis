package allawala.chassis.core.util

import allawala.chassis.core.exception.{DomainException, ValidationException}
import allawala.chassis.core.model.{ErrorLog, ValidationEnvelope}
import allawala.chassis.i18n.service.I18nService
import com.typesafe.scalalogging.StrictLogging

trait LogWrapper extends StrictLogging {
  def i18nService: I18nService

  protected def logIt(e: DomainException) = {
    import io.circe.generic.auto._
    import io.circe.syntax._

    val errorLog = ErrorLog(
      errorType = e.errorType,
      errorCode = e.errorCode,
      errorMessage = i18nService.getForDefaultLocale(e.errorCode, e.messageParameters),
      thread = e.thread,
      payload = e.logMap.view.mapValues(_.toString).toMap,
      details = getErrorPayload(e)
    )

    logger.error(errorLog.asJson.noSpaces, e)
    Option(e.cause).foreach(logger.error("caused by", _))
  }

  def logErrorEither(f: => Either[DomainException, _]): Unit = {
    f.left.foreach(logIt)
  }

  private def getErrorPayload(e: DomainException) = {
    e match {
      case ve: ValidationException =>
        ve.validationErrors.toList.groupBy(_.field).view.mapValues { grouped =>
          grouped map { g =>
            val message = i18nService.getForDefaultLocale(g.code, g.parameters)
            ValidationEnvelope(g.code, message)
          }
        }.toMap
      case _ => Map.empty[String, List[ValidationEnvelope]]
    }
  }

}
