package allawala.chassis.core
import com.google.inject.Module

object Boot extends Microservice with App {
  override def getModules: List[Module] = List.empty[Module]
  run()
}
