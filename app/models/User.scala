package models

case class User(id: String, name: String, pass: String, conversations: Seq[String])

object User {
  import play.api.libs.json._

  implicit object UserWrites extends OWrites[User] {
    def writes(user: User): JsObject = Json.obj(
      "_id" -> user.id,
      "name" -> user.name,
      "pass" -> user.pass,
      "conversations" -> user.conversations)
  }

  implicit object UserReads extends Reads[User] {
    def reads(json: JsValue): JsResult[User] = json match {
      case obj: JsObject => try
        JsSuccess(User(
          (obj \ "_id" \ "$oid").as[String],
          (obj \ "name").as[String],
          (obj \ "pass").as[String],
          (obj \ "conversations").asOpt[JsArray].getOrElse(JsArray()).value.map(_.as[String])
        ))
      catch {
        case cause: Throwable => JsError(cause.getMessage)
      }
      case _ => JsError("expected.jsobject")
    }
  }
}