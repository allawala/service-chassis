package allawala

import akka.http.scaladsl.server.Route
import allawala.modules.{AkkaHttp, BootModule}
import com.google.inject.{Guice, Injector, Module, Stage}
import net.codingwell.scalaguice.InjectorExtensions._

trait Boot {
  def getModules: List[Module]
  def getRoute: Route

  def run(): Unit = {
    val modules: List[Module] = BootModule() :: getModules
    val injector: Injector = Guice.createInjector(Stage.PRODUCTION, modules:_*)
    val akkaHttp = injector.instance[AkkaHttp]
    akkaHttp.run(getRoute)
  }
}
