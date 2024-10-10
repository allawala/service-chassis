package allawala.chassis.auth.shiro.module

import jakarta.inject.Singleton

import allawala.chassis.auth.shiro.ShiroLifecycleListener
import allawala.chassis.auth.shiro.realm.{JWTRealm, UsernamePasswordRealm}
import com.google.inject.Provides
import com.google.inject.multibindings.Multibinder
import com.google.inject.name.Names
import org.apache.shiro.guice.ShiroModule
import org.apache.shiro.realm.Realm

class ShiroAuthModule extends ShiroModule {
  override def configureShiro(): Unit = {
    /*
     This is a stateless rest api and we do not want the subjects identity and auth state to be stored in a session
     automatically.

     IMPORTANT CAVEAT
     This will disable Shiro's own implementations from using Sessions as a storage strategy. It DOES NOT disable Sessions
     entirely. A session will still be created if any of your own code explicitly calls subject.getSession() or
     subject.getSession(true)
    */
    bindConstant.annotatedWith(Names.named("shiro.sessionStorageEnabled")).to(false)
    bind(classOf[ShiroLifecycleListener]).asEagerSingleton()

    bindRealms()
  }

  protected def bindRealms(): Unit = {

    val multibinder = Multibinder.newSetBinder(binder, classOf[Realm])
    multibinder.addBinding().to(classOf[JWTRealm])
    multibinder.addBinding().to(classOf[UsernamePasswordRealm])
  }
}

object ShiroAuthModule {

  @Provides
  @Singleton
  def getJWTRealm(): JWTRealm = {
    new JWTRealm
  }
}
