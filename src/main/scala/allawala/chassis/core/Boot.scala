package allawala.chassis.core
import allawala.chassis.core.module.ChassisModule

object Boot extends Microservice with App {
  override def module: ChassisModule = new ChassisModule() {}
  run()
}
