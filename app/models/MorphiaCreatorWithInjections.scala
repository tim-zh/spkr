package models

import org.mongodb.morphia.mapping.DefaultCreator
import play.api.Play

class MorphiaCreatorWithInjections extends DefaultCreator {
  override def createInstance[T](clazz: Class[T]): T =
    try
      Play.current.injector.instanceOf(clazz)
    catch {
      case e: Exception =>
        super.createInstance(clazz)
    }
}
