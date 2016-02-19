import play.api.Play
import play.api.data.FormError
import play.api.libs.json.Json
import play.api.mvc.RequestHeader

import scala.reflect.ClassTag

package object controllers {
  def jsonErrors(message: (String, String)) = Json.arr(Json.obj(message._1 -> message._2))

  def jsonErrors(messages: Seq[(String, String)]) = Json.arr(messages.map { case (key, message) => Json.obj(key -> message) })

  def jsonErrors[T <: FormError](messages: Seq[T])(implicit tag: ClassTag[T]) = Json.arr(messages.map { err => Json.obj(err.key -> err.message) })

  def getUserFromRequest(req: RequestHeader) = req.session.get("sname").flatMap(Play.current.injector.instanceOf[models.Dao].user.get)

  case class Login(name: String, pass: String)

  case class Register(name: String, pass: String, pass2: String) {
    def validate =
      if (pass != pass2)
        Seq("pass" -> "passwords don't match")
      else
        Nil
  }

  case class ChatMessage(chatId: String, message: String)
}
