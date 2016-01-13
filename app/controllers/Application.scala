package controllers

import models.MongoDao
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc._
import reactivemongo.core.errors.ReactiveMongoException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.reflect.ClassTag

class Application extends Controller {
  val dao = MongoDao

  def authenticate() = Action.async { implicit request =>
    Form(mapping("name" -> nonEmptyText, "pass" -> nonEmptyText)(Login.apply)(Login.unapply)).bindFromRequest.fold(
      bad =>
        Future.successful(Ok(jsonErrors(bad.errors))),
      form =>
        dao.getUser(form.name, form.pass).map { user =>
          if (user.isDefined)
            Ok(Json.obj("sid" -> user.get.id))
          else
            BadRequest(jsonErrors("user" -> "not found"))
        }
    )
  }

  def addUser() = Action.async { implicit request =>
    Form(mapping("name" -> nonEmptyText, "pass" -> nonEmptyText, "pass2" -> nonEmptyText)(Register.apply)(Register.unapply)).bindFromRequest.fold(
      bad =>
        Future.successful(BadRequest(jsonErrors(bad.errors))),
      form =>
        dao.addUser(form.name, form.pass).flatMap { writeResult =>
          if (writeResult.n != 1)
            Future.successful(BadRequest(jsonErrors("user" -> "not found")))
          dao.getUser(form.name, form.pass).map { user =>
            if (user.isDefined)
              Ok(Json.obj("sid" -> user.get.id))
            else
              BadRequest(jsonErrors("user" -> "not found"))
          }
        } recover {
          case e: ReactiveMongoException =>
            BadRequest(jsonErrors("user" -> e.message))
        }
    )
  }


  private def jsonErrors(messages: (String, String)*) = Json.arr(messages.map { case (key, message) => Json.obj(key -> message) })

  private def jsonErrors[T <: FormError](messages: Seq[T])(implicit tag: ClassTag[T]) = Json.arr(messages.map { err => Json.obj(err.key -> err.message) })

  private case class Login(name: String, pass: String)

  private case class Register(name: String, pass: String, pass2: String) {
    def validate =
      if (pass != pass2)
        Seq("pass" -> "passwords don't match")
      else
        Nil
  }
}
