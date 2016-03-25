package models.entities

import java.text.SimpleDateFormat
import java.util.Date

import org.bson.types.ObjectId
import org.mongodb.morphia.annotations._
import play.api.libs.json.{JsObject, Json}

@Embedded
case class Message(var id: Long,
                   var text: String,
                   var audioId: String,
                   var author: ObjectId,
                   var date: Date) {
  def this() = this(-1, "", null, null, null)

  def json(authorName: String): JsObject = Json.obj(
    "id" -> id,
    "author" -> String.valueOf(authorName),
    "date" -> Message.dateFormat.format(date),
    "text" -> text,
    "audio" -> audioId
  )
}

object Message {
  val dateFormat: SimpleDateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm:ss")
}
