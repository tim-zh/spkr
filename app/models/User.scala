package models

case class User(id: String, name: String, pass: String)

object User {
  import play.api.libs.json._

  implicit object UserWrites extends OWrites[User] {
    def writes(user: User): JsObject = Json.obj(
      "_id" -> user.id,
      "name" -> user.name,
      "pass" -> user.pass)
  }

  implicit object UserReads extends Reads[User] {
    def reads(json: JsValue): JsResult[User] = json match {
      case obj: JsObject => try
        JsSuccess(User(
          (obj \ "_id" \ "$oid").as[String],
          (obj \ "name").as[String],
          (obj \ "pass").as[String]
        ))
      catch {
        case cause: Throwable => JsError(cause.getMessage)
      }
      case _ => JsError("expected.jsobject")
    }
  }
}