package allawala.chassis.service

import akka.http.scaladsl.server.Route
import allawala.chassis.http.model.AkkaHttp
import allawala.chassis.http.module.BootModule
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