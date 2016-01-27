package models

import java.text.SimpleDateFormat

import com.google.inject.AbstractModule
import com.google.inject.name.Names

class ModelsModule extends AbstractModule {
  val defaultDateFormat = "dd MMM yyyy HH:mm:ss"

  override def configure() = {
    bind(classOf[Dao]).to(classOf[DaoImpl]).asEagerSingleton()

    bind(classOf[SimpleDateFormat]).annotatedWith(Names.named("default")).toInstance(new SimpleDateFormat(defaultDateFormat))
  }
}
