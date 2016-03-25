package util

import play.api.inject.{BindingKey, Injector}

import scala.reflect.ClassTag

case class InjectorMerger(var first: Injector, var second: Injector) extends Injector {
  def instanceOf[T](implicit ct: ClassTag[T]) = instanceOf(ct.runtimeClass.asInstanceOf[Class[T]])

  def instanceOf[T](clazz: Class[T]): T =
    try
      first.instanceOf(clazz)
    catch {
      case e: Exception =>
        second.instanceOf(clazz)
    }

  def instanceOf[T](key: BindingKey[T]): T =
    try
      first.instanceOf(key)
    catch {
      case e: Exception =>
        second.instanceOf(key)
    }
}
