package models.entities

import java.text.SimpleDateFormat
import java.util.Date

import com.google.inject.Inject
import com.google.inject.name.Named
import models.Dao
import org.bson.types.ObjectId
import org.mongodb.morphia.annotations.{Transient, Embedded}
import play.api.libs.json.{JsObject, JsString, Json}

@Embedded
case class Message(var text: String,
                   var audio: Array[Byte],
                   var author: ObjectId,
                   var date: Date) {
  @Transient
  @Inject
  var dao: Dao = _

  @Transient
  @Inject
  @Named("default")
  var dateFormat: SimpleDateFormat = _

  def this() = this("", null, null, null)

  def json: JsObject = Json.obj(
    "author" -> String.valueOf(dao.user.get(author).map(_.name).getOrElse("")),
    "date" -> dateFormat.format(date),
    "text" -> JsString(text)
  )
}
