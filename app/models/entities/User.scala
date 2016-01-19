package models.entities

import org.bson.types.ObjectId
import org.mongodb.morphia.annotations._

@Entity("users")
@Index(fields = Array(new Field(value = "name")), options = new IndexOptions(unique = true))
case class User(
                   var name: String,
                   var pass: String,
                   @Reference
                   var conversations: java.util.List[String]) {
  @Id
  var id: ObjectId = _

  def this() = {
    this("", "", new java.util.ArrayList[String]())
  }
}
