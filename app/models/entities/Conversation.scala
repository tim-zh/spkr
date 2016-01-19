package models.entities

import org.bson.types.ObjectId
import org.mongodb.morphia.annotations._

@Entity(value = "conversations", noClassnameStored = true)
case class Conversation(
		                       var title: String,
		                       var history: java.util.List[String],
		                       @Reference(idOnly = true)
		                       var users: java.util.List[ObjectId]) {
	@Id
	var id: ObjectId = _
	@Version
	var version: Long = _

	def this() = this("", new java.util.ArrayList[String](), new java.util.ArrayList[ObjectId]())
}
