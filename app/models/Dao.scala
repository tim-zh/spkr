package models

trait Dao {
  val datastore = DS("localhost", 27017, "test")
}
