package allawala.chassis.auth.shiro

import javax.inject.{Inject, Named}

import allawala.chassis.core.exception.InitializationException
import allawala.chassis.http.lifecycle.LifecycleAware
import org.apache.shiro.SecurityUtils
import org.apache.shiro.mgt.SecurityManager

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class ShiroLifecycleListener @Inject()(
                                        @Named("default-dispatcher") implicit val ec: ExecutionContext,
                                        val securityManager: SecurityManager
                                      ) extends LifecycleAware {

  override def preStart(): Future[Either[InitializationException, Unit]] = {
    Future {
      Try {
        SecurityUtils.setSecurityManager(securityManager)
      }.toEither.left.map(e => InitializationException(logMap = Map("listener" -> "ShiroLifecycleListener"), cause = e))
    }
  }

  override def postStart(): Future[Either[InitializationException, Unit]] = {
    Future.successful(Right(()))
  }

  override def preStop(): Future[Either[InitializationException, Unit]] = {
    Future.successful(Right(()))
  }
}
