package models.entities

import org.bson.types.ObjectId
import org.mongodb.morphia.annotations._

@Entity(value = "users", noClassnameStored = true)
case class User(@Indexed(options = new IndexOptions(unique = true))
                var name: String,
                var pass: String,
                var chats: java.util.List[ObjectId]) {
  @Id
  var id: ObjectId = _

  def this() = this("", "", new java.util.ArrayList[ObjectId]())
}
