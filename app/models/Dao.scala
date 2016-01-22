package models

trait Dao[T] {
  val datastore: DSImpl

  def save(entity: T) = datastore.persist(entity)
}

object Dao {
  val datastore = DS("localhost", 27017, "test")
}