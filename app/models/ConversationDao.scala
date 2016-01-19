package models

import models.entities.{User, Conversation}
import org.bson.types.ObjectId
import scala.collection.JavaConversions._

object ConversationDao extends Dao {
	def add(title: String, users: Seq[User]) = {
		val conversation = Conversation(title, Seq(), users.map(_.id))
		val result = datastore.persist(conversation)
		users.foreach { user =>
			user.conversations :+= conversation.id
			UserDao.update(user)
		}
		result
	}

	def get(id: ObjectId) =
		Option(datastore.get(classOf[Conversation], id))
}
