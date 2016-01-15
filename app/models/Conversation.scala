package models

case class Conversation(id: String, title: String, usernames: Seq[String], history: Seq[String])

object Conversation {
  import play.api.libs.json._

  implicit object ConversationWrites extends OWrites[Conversation] {
    def writes(conversation: Conversation): JsObject = Json.obj(
      "_id" -> conversation.id,
      "title" -> conversation.title,
      "users" -> conversation.usernames,
      "history" -> conversation.history)
  }

  implicit object ConversationReads extends Reads[Conversation] {
    def reads(json: JsValue): JsResult[Conversation] = json match {
      case obj: JsObject => try
        JsSuccess(Conversation(
          (obj \ "_id" \ "$oid").as[String],
          (obj \ "title").as[String],
          (obj \ "users").as[JsArray].value.map(_.as[String]),
          (obj \ "history").as[JsArray].value.map(_.as[String])
        ))
      catch {
        case cause: Throwable => JsError(cause.getMessage)
      }
      case _ => JsError("expected.jsobject")
    }
  }
}