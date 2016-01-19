package models.entities

import org.bson.types.ObjectId
import org.mongodb.morphia.annotations._

@Entity("conversations")
case class Conversation(
                           var title: String,
                           @Reference
                           var usernames: java.util.List[String],
                           var history: java.util.List[String]) {
  @Id
  var id: ObjectId = _

  def this() = {
    this("", new java.util.ArrayList[String](), new java.util.ArrayList[String]())
  }
}
