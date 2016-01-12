package controllers

import play.api._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc._
import reactivemongo.bson.BSONDocument
import scala.concurrent.ExecutionContext.Implicits.global

class Application extends Controller {
  val dao = Global.dao.get

  def index = Action.async {
    dao.getUsers(BSONDocument()).map { list =>
      Ok(list.size + "")
    }
  }

  def login = Action { implicit request =>
    Form(mapping("name" -> nonEmptyText, "pass" -> nonEmptyText)(Login.apply)(Login.unapply)).bindFromRequest.fold(
      bad => {
        Ok(Json.arr(bad.errors.map(err => Json.obj(err.key -> err.message))))
      },
      form => {
        Ok(Json.obj("sid" -> 1))
      }
    )
  }

  def register = Action { implicit request =>
    Form(mapping("name" -> nonEmptyText, "pass" -> nonEmptyText, "pass2" -> nonEmptyText)(Register.apply)(Register.unapply)).bindFromRequest.fold(
      bad => {
        Ok(Json.arr(bad.errors.map(err => Json.obj(err.key -> err.message))))
      },
      form => {
        Ok(Json.obj("sid" -> 1))
      }
    )
  }


  private case class Login(name: String, pass: String)

  private case class Register(name: String, pass: String, pass2: String)
}
