package allawala.chassis.core.model

/*
  Logging payload for any any thing not handled by the Http request error logging.
*/
case class ErrorLog(
                     errorType: ErrorType,
                     errorCode: String,
                     errorMessage: String,
                     thread: Option[String],
                     payload: Map[String, String] = Map.empty[String, String]
                   )
