import allawala.chassis.core.exception.DomainException

import scala.concurrent.Future

package object allawala {
  type ResponseFE[T] = Future[Either[DomainException, T]]
  type ResponseE[T] = Either[DomainException, T]
}
