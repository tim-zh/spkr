package models.entities

import org.bson.types.ObjectId
import org.mongodb.morphia.annotations.{Entity, Id}

@Entity(value = "audios", noClassnameStored = true)
case class Audio(data: Array[Byte]) {
	@Id
	var id: ObjectId = _

	def this() = this(null)
}
