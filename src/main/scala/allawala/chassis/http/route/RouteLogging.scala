package allawala.chassis.http.route

import akka.http.scaladsl.model.HttpRequest
import allawala.chassis.core.exception.DomainException
import allawala.chassis.core.model.HttpErrorLog
import com.typesafe.scalalogging.StrictLogging

trait RouteLogging extends StrictLogging {
  def logError(request: HttpRequest, e: DomainException): Unit = {
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
}
