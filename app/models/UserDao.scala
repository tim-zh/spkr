package models

import models.entities.User
import scala.collection.JavaConversions._

object UserDao extends Dao[User] {
	def get(name: String) =
		Option(datastore.find(classOf[User], "name", name).get())

	def list(nameQuery: String, limit: Int = 20) =
		datastore.createQuery(classOf[User]).field("name").startsWithIgnoreCase(nameQuery).limit(limit).asList()

	def add(name: String, pass: String) =
		datastore.persist(User(name, pass, Seq()))

	def update(user: User) =
		datastore.persist(user)
}
