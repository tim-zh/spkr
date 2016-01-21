package models

trait Dao[T] {
  val datastore = DS("localhost", 27017, "test")

  def save(entity: T) = datastore.persist(entity)
}
