package allawala.chassis.core.exception

case class ErrorEnvelope(
                          errorType: ErrorType,
                          correlationId: String,
                          errorCode: String,
                          errorMessage: String,
                          payload: Map[String, String] = Map.empty[String, String]
                        )
