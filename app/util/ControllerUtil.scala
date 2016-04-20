package util

import play.api.Play
import play.api.data.FormError
import play.api.libs.json.Json
import play.api.mvc.{Results, RequestHeader}

trait ControllerUtil {
  object Secured extends play.api.mvc.Security.AuthenticatedBuilder(new ControllerUtil {}.getUserFromRequest)

  def badRequestJson(message: (String, String)) = Results.BadRequest {
    Json.arr(Json.obj(message._1 -> message._2))
  }

  def badRequestJson[T <: FormError](messages: Seq[T]) = Results.BadRequest {
    Json.arr(messages.map { err => Json.obj(err.key -> err.message) })
  }

  def getUserFromRequest(req: RequestHeader) =
    req.session.get("sname").flatMap(Play.current.injector.instanceOf[models.Dao].user.get)
}
