package allawala.chassis.core.rejection

import akka.http.scaladsl.server.Rejection
import allawala.chassis.core.exception.DomainException

import scala.language.implicitConversions

case class DomainRejection(ex: DomainException) extends Rejection

object DomainRejection {
  /*
    Turn the domain exception into something we handle as a rejection in directives instead of throwing exceptions
  */
  implicit def domainExceptionToRejection(e: DomainException): DomainRejection = {
    DomainRejection(e)
  }
}