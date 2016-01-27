package models.entities

import java.text.SimpleDateFormat
import java.util.Date

import com.google.inject.Inject
import com.google.inject.name.Named
import models.Dao
import org.bson.types.ObjectId
import org.mongodb.morphia.annotations.Embedded
import play.api.libs.json.{JsObject, JsString, Json}

sealed abstract class Message(
                                 var category: String,
                                 var _author: ObjectId,
                                 var _date: Date) {
  @Inject
  var dao: Dao = _

  @Inject
  @Named("default")
  var dateFormat: SimpleDateFormat = _

  def json: JsObject = Json.obj(
    "category" -> category,
    "author" -> String.valueOf(dao.user.get(_author).map(_.name).getOrElse("")),
    "date" -> dateFormat.format(_date))
}

@Embedded
case class TextMessage(
                          var text: String,
                          var author: ObjectId,
                          var date: Date) extends Message("text", author, date) {
  def this() = this("", null, null)

  override def json = super.json + ("text" -> JsString(text))
}
