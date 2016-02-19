package models

import java.text.SimpleDateFormat

import com.google.inject.{Singleton, Provides, AbstractModule}
import com.google.inject.name.Names
import com.mongodb.MongoClient

class ModelsModule extends AbstractModule {
  val defaultDateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm:ss")

  override def configure() = {
    bind(classOf[Dao]).to(classOf[DaoImpl]).asEagerSingleton()

    bind(classOf[SimpleDateFormat]).annotatedWith(Names.named("default")).toInstance(defaultDateFormat)
  }

  @Provides
  @Singleton
  def createMongoClient() = new MongoClient("localhost", 27017)
}
