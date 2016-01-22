package controllers

import models.UserDao
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{Action, Controller}

class Auth extends Controller {
  val userDao = UserDao

  def authenticate() = Action { implicit request =>
    Form(mapping("name" -> nonEmptyText, "pass" -> nonEmptyText)(Login.apply)(Login.unapply)).bindFromRequest.fold(
      bad =>
        BadRequest(jsonErrors(bad.errors)),
      form => {
        val user = userDao.get(form.name)
        if (user.isDefined && user.get.pass == form.pass)
          Ok("").withSession("sname" -> user.get.name)
        else
          BadRequest(jsonErrors("user" -> "not found"))
      }
    )
  }

  def addUser() = Action { implicit request =>
    Form(mapping("name" -> nonEmptyText, "pass" -> nonEmptyText, "pass2" -> nonEmptyText)(Register.apply)(Register.unapply)).bindFromRequest.fold(
      bad =>
        BadRequest(jsonErrors(bad.errors)),
      form => {
        val errors = form.validate
        if (errors.nonEmpty)
          BadRequest(jsonErrors(errors))
        else {
          val result = userDao.add(form.name, form.pass)
          if (result.isLeft)
            BadRequest(jsonErrors(result.left.get))
          else
            Ok("").withSession("sname" -> form.name)
        }
      }
    )
  }
}
