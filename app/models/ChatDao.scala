package models

import models.entities.{User, Chat}
import org.bson.types.ObjectId
import scala.collection.JavaConversions._

object ChatDao extends Dao[Chat] {
	override val datastore = Dao.datastore

	def add(title: String, users: Seq[User]) = {
		val chat = Chat(title, Seq(), users.map(_.id))
		val result = datastore.persist(chat)
		users.foreach { user =>
			user.chats :+= chat.id
			datastore.persist(user)
		}
		result
	}

	def get(id: String) =
		Option(datastore.get(classOf[Chat], new ObjectId(id)))
}
