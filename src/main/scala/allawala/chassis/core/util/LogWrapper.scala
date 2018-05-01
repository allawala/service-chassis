package allawala.chassis.core.util

import allawala.chassis.core.exception.DomainException
import allawala.chassis.core.model.ErrorLog
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
      errorMessage = i18nService.getDefaultLocale(e.errorCode, Seq.empty),
      thread = e.thread,
      payload = e.logMap.mapValues(_.toString)
    )

    logger.error(errorLog.asJson.noSpaces, e)
  }

  def logErrorEither(f: => Either[DomainException, _]): Unit = {
    f.left.foreach(logIt)
  }
}
