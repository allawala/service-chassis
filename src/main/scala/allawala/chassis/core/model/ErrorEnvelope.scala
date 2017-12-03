package allawala.chassis.core.model

/*
  Response payload on any error
*/
case class ErrorEnvelope(
                          errorType: ErrorType,
                          correlationId: String,
                          errorCode: String,
                          errorMessage: String,
                          details: Map[String, List[ValidationEnvelope]] = Map.empty
                        )
