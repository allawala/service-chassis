package allawala.chassis.http.lifecycle

import allawala.chassis.core.exception.InitializationException

import scala.concurrent.Future

/*
  Base do nothing implementation that can be used if the inheriting life cycle listener does not need to implement all three methods
 */
class BaseLifecycleAware extends LifecycleAware {

  override def preStart(): Future[Either[InitializationException, Unit]] = Future.successful(Right(()))

  override def postStart(): Future[Either[InitializationException, Unit]] = Future.successful(Right(()))

  override def preStop(): Future[Either[InitializationException, Unit]] = Future.successful(Right(()))
}
