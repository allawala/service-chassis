package allawala.chassis.core.exception

case class HttpErrorLog(
                         method: String,
                         uri: String,
                         errorType: ErrorType,
                         errorCode: String,
                         errorMessage: String,
                         thread: Option[String],
                         payload: Map[String, String] = Map.empty[String, String]
                       )
