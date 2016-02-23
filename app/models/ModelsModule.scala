package models

import java.text.SimpleDateFormat

import com.google.inject.{Injector, Singleton, Provides, AbstractModule}
import com.google.inject.name.Names
import com.mongodb.MongoClient
import org.mongodb.morphia.Morphia

class ModelsModule extends AbstractModule {
  val defaultDateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm:ss")

  override def configure() = {
    bind(classOf[Dao]).to(classOf[DaoImpl]).asEagerSingleton()

    bind(classOf[SimpleDateFormat]).annotatedWith(Names.named("default")).toInstance(defaultDateFormat)
  }

  @Provides
  @Singleton
  def createMongoClient(): MongoClient = new MongoClient("localhost", 27017)

  @Provides
  @Singleton
  def createDSImpl(injector: Injector): DSImpl = {
    val morphia = new Morphia
    morphia.getMapper.getOptions.setObjectFactory(new MorphiaCreatorWithInjections)
    morphia.mapPackage("app.models.entities")
    val mongoClient = injector.getInstance(classOf[MongoClient])
    val result = new DSImpl(morphia, mongoClient, "test")
    result.ensureIndexes()
    result
  }
}
