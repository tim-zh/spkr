package models.entities

import java.util.Date

import models.StaticInjections
import org.bson.types.ObjectId
import org.mongodb.morphia.annotations.Embedded
import play.api.libs.json.{JsObject, JsString, Json}

sealed abstract class Message(
                                 var category: String,
                                 var _author: ObjectId,
                                 var _date: Date) {
  def json: JsObject = Json.obj(
    "category" -> category,
    "author" -> String.valueOf(StaticInjections.dao.user.get(_author).map(_.name).getOrElse("")),
    "date" -> StaticInjections.dateFormat.format(_date))
}

@Embedded
case class TextMessage(
                          var text: String,
                          var author: ObjectId,
                          var date: Date) extends Message("text", author, date) {
  def this() = this("", null, null)

  override def json = super.json + ("text" -> JsString(text))
}
