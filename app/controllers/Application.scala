package controllers

import models.MongoDao
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.{JsString, JsArray, Json}
import play.api.mvc._
import reactivemongo.core.errors.ReactiveMongoException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

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
      { form =>
        val errors = form.validate
        if (errors.nonEmpty)
          Future.successful(BadRequest(jsonErrors(errors)))
        else
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
      }
    )
  }

  def searchUser(query: String) = Action.async {
    dao.getUsers(query).map { users =>
      val jsUsers = JsArray(users.map(user => JsString(user.name)))
      Ok(jsUsers)
    }
  }
}
