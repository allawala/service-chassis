package allawala.chassis.http.lifecycle

import allawala.chassis.core.exception.InitializationException

import scala.concurrent.Future

trait LifecycleAware {
  /*
    perform actions to be performed before the http server is bound and starts listening for incoming requests
    This should avoid performing any long running operations
   */
  def preStart(): Future[Either[InitializationException, Unit]]
}
