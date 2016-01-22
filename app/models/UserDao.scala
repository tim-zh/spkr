package models

import com.mongodb.DuplicateKeyException
import models.entities.User
import scala.collection.JavaConversions._

object UserDao extends Dao[User] {
	override val datastore = Dao.datastore

	def get(name: String) =
		Option(datastore.find(classOf[User], "name", name).get())

	def list(nameQuery: String, limit: Int = 20) =
		datastore.createQuery(classOf[User]).field("name").startsWithIgnoreCase(nameQuery).limit(limit).asList()

	def add(name: String, pass: String) =
		try
			Right(datastore.persist(User(name, pass, Seq())))
		catch {
			case e: DuplicateKeyException =>
				Left("user" -> ("duplicate name " + name))
		}
}
