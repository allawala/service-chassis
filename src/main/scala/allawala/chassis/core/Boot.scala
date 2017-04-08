package allawala.chassis.core

import akka.http.scaladsl.server.Route
import allawala.chassis.core.module.BootModule
import allawala.chassis.http.model.AkkaHttp
import com.google.inject.{Guice, Injector, Module, Stage}
import net.codingwell.scalaguice.InjectorExtensions._

// TODO revert injector back to val and refactor the tests
// TODO remove the getRoute as we are using RouteRegistry (this will break the tests)
trait Boot {
  def getModules: List[Module]
  def getRoute: Route
  protected var injector: Injector = _


  def run(): Unit = {
    val modules: List[Module] = BootModule() :: getModules
    injector = Guice.createInjector(Stage.PRODUCTION, modules:_*)
    val akkaHttp = injector.instance[AkkaHttp]
    akkaHttp.run()
  }
}
