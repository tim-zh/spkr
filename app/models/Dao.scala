package models

import org.bson.types.ObjectId

import scala.reflect._

abstract class Dao[T: ClassTag] {
  val datastore: DSImpl

  def get(id: ObjectId) =
    Option(datastore.get(classTag[T].runtimeClass, id).asInstanceOf[T])

  def save(entity: T) = datastore.persist(entity)
}

object Dao {
  val datastore = DS("localhost", 27017, "test")
}