package allawala.chassis.core

import allawala.chassis.core.module.ChassisModule
import allawala.chassis.http.service.AkkaHttpService
import com.google.inject.{Guice, Injector, Module, Stage}
import net.codingwell.scalaguice.InjectorExtensions._

trait Microservice {
  def getModules: List[Module]
  private val modules: List[Module] = ChassisModule() :: getModules
  protected val injector: Injector = Guice.createInjector(Stage.PRODUCTION, modules:_*)


  def run(): Unit = {
    val akkaHttp = injector.instance[AkkaHttpService]
    akkaHttp.run()
  }
}
