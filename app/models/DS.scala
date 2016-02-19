package models

import com.mongodb.MongoClient
import org.mongodb.morphia.Morphia
import play.api.Play

object DS {
  private val morphia = new Morphia
  morphia.getMapper.getOptions.setObjectFactory(new MorphiaCreatorWithInjections)
  morphia.mapPackage("app.models.entities")

  lazy val datastore = apply("test")

  def apply(dbName: String) = {
    val mongoClient = Play.current.injector.instanceOf[MongoClient]
    val result = new DSImpl(morphia, mongoClient, dbName)
    result.ensureIndexes()
    result
  }
}
