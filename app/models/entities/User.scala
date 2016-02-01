package models.entities

import org.bson.types.ObjectId
import org.mongodb.morphia.annotations._

@Entity(value = "users", noClassnameStored = true)
case class User(@Indexed(options = new IndexOptions(unique = true))
                var name: String,
                var pass: String,
                @Reference //idOnly doesn't work
                var chats: java.util.List[ObjectId]) {
  @Id
  var id: ObjectId = _

  def this() = this("", "", new java.util.ArrayList[ObjectId]())
}
