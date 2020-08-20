package allawala.chassis.core.module

import allawala.chassis.util.{Registry, StringConverters}
import com.google.inject.TypeLiteral
import com.google.inject.spi.{InjectionListener, TypeEncounter, TypeListener}
import javax.inject.Provider

import scala.reflect.{ClassTag, classTag}

abstract class HasTypeListener[T: ClassTag, U <: Registry[T]] extends TypeListener {
  val registryProvider: Provider[U]

  override def hear[I](typeLiteral: TypeLiteral[I], encounter: TypeEncounter[I]): Unit = {
    val clazz = typeLiteral.getRawType

    if (classTag[T].runtimeClass.isAssignableFrom(clazz)) encounter.register(new InjectionListener[I] {

      override def afterInjection(injectee: I): Unit = {
        val entry: T = injectee.asInstanceOf[T]
        val name = StringConverters.upperCamelToLowerHyphen.convert(entry.getClass.getSimpleName)
        registryProvider.get().register(name, entry)
      }
    })
  }
}
