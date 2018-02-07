package allawala.chassis.core.exception

import akka.http.scaladsl.model.StatusCode
import allawala.chassis.core.model.ErrorType

trait DomainException extends Exception {
  def statusCode: StatusCode

  def errorType: ErrorType

  def cause: Throwable

  def errorCode: String

  /**
    *
    * messageParameters: values to be substituted in the messages file
    */
  def messageParameters: Seq[AnyRef] = Seq.empty

  /**
    *   logMap: any key value pair that need to be logged as part of the [[allawala.chassis.core.model.HttpErrorLog]] but is not required to be part of the
    *   error response in the [[allawala.chassis.core.model.ErrorEnvelope]]
    */
  def logMap: Map[String, AnyRef] = Map.empty[String, AnyRef]

  /*
   For the most part, exceptions will be logged globally at the outer edges where the logging thread will most likely be the
   dispatcher thread. However, the actual failure might have occurred on a different thread. Hence we capture this information
   as it might be useful in debugging errors.
  */
  val thread: Option[String] = Some(Thread.currentThread().getName)

  override def getMessage: String = Option(cause).map(_.getMessage).getOrElse(errorCode)

  override def getCause: Throwable = cause
}
