package models.entities

import java.text.SimpleDateFormat
import java.util.Date

import com.google.inject.Inject
import com.google.inject.name.Named
import models.Dao
import org.bson.types.ObjectId
import org.mongodb.morphia.annotations._
import play.api.libs.json.{JsObject, Json}

@Embedded
case class Message(var id: Long,
                   var text: String,
                   var audioId: String,
                   var author: ObjectId,
                   var date: Date) {
  @Transient
  @Inject
  @Named("default")
  var dateFormat: SimpleDateFormat = _

  def this() = this(-1, "", null, null, null)

  def json(authorName: String): JsObject = Json.obj(
    "id" -> id,
    "author" -> String.valueOf(authorName),
    "date" -> dateFormat.format(date),
    "text" -> text,
    "audio" -> audioId
  )
}
