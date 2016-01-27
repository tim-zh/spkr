package models

import com.mongodb.MongoClient
import org.mongodb.morphia.Morphia

object DS {
  private val morphia = new Morphia
  morphia.getMapper.getOptions.setObjectFactory(new MorphiaCreatorWithInjections)
  morphia.mapPackage("app.models.entities")

  lazy val datastore = apply("localhost", 27017, "test")

  def apply(host: String, port: Int, dbName: String) = {
    val result = new DSImpl(morphia, new MongoClient(host, port), dbName)
    result.ensureIndexes()
    result
  }
}
