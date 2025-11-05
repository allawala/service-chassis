package allawala.chassis.core

import allawala.chassis.core.module.ChassisModule
import allawala.chassis.http.service.PekkoHttpService
import com.google.inject.{Guice, Injector, Stage}
import net.codingwell.scalaguice.InjectorExtensions._

trait Microservice {
  def module: ChassisModule
  protected val injector: Injector = Guice.createInjector(Stage.PRODUCTION, module)


  def run(): Unit = {
    val pekkoHttp = injector.instance[PekkoHttpService]
    pekkoHttp.run()
  }
}
